# Diseño: Migración de Carga Consolidada a Stored Procedures (SQL dinámico)

> Estado: **DISEÑO — pendiente de aprobación, sin código aún**
> Alcance: `HeaderConfigurationCommandServiceImpl` — los 3 métodos de escritura masiva.
> Decisiones tomadas: (1) **SP puro con SQL dinámico**, (2) **feature flag + parallel-run**, (3) diseñar los 3 antes de codificar.

---

## 1. Objetivo

Reemplazar los bucles **fila-por-fila** (`jdbcTemplate.update()` por registro → decenas de miles de round-trips) por **stored procedures set-based** que escriben desde una **tabla staging**, sin alterar el comportamiento observable.

Métodos en alcance (todos en `HeaderConfigurationCommandServiceImpl.java`):

| Método | Líneas hoy | Rol | SP destino |
|---|---|---|---|
| `importDataToTable` | 700–960 | UPSERT principal (INICIAL) y Fase 1 diaria (ACTUALIZACION) | `sp_import_upsert` |
| `importDailyData` Fase 2 | 1758–1865 | UPDATE de INICIAL por link field (con COALESCE) | `sp_import_update` (coalesce=1) |
| `updateComplementaryDataInTable` | 1368–1505 | UPDATE de columnas existentes por link field | `sp_import_update` (coalesce=0) |

Fuera de alcance (esta iteración): snapshot (ya en SP V13/V14), `CustomerSyncService` (ya batched; candidato a SP fijo en fase posterior), DDL dinámico y helpers de metadatos.

---

## 2. Contrato invariante (lo que NO puede cambiar)

El SP solo asume responsabilidad de **escritura**. Todo lo demás se queda **idéntico en Java**:

1. **Resolución de cabeceras**: `sourceField`, `aliases`, `normalizeHeaderName` (lower, sin acentos, espacios→`_`, sin especiales).
2. **Regex** `applyRegexTransformation` (`group(1)` o `group(0)`), ej. `D000080413598 → 80413598`.
3. **Conversión/validación de tipo** vía `DataTypeValidator` (fecha flexible, decimal, BIT…).
4. **Validación de obligatorios** (lanza por fila; las demás continúan).
5. **Matching UPSERT de 2 niveles**: `identity_code` (lower+trim) y, si vacío, **nombre**. → `existing_id` lo calcula Java.
6. **COALESCE** en update de INICIAL diaria (mantener valor previo si el nuevo es NULL).
7. **Conteos**: `insertedRows / updatedRows / notFoundRows / failedRows`.
8. **Claves del Map de respuesta** que consume el frontend (ver §6).
9. **Sincronización post-carga** (INICIAL = full; DIARIA = selectiva por códigos; + `reapplyInvalidPhoneContactInactivation`).
10. **Tolerancia a fila inválida**: se cuenta y se salta; el resto continúa.

> **Principio clave:** Java **valida y convierte antes** de poblar staging. Las filas inválidas se cuentan como `failed` en Java y **nunca llegan al SP**. Así el SP solo ve filas limpias y su escritura set-based no puede fallar por fila → se preserva "salta la mala, sigue con las demás".

---

## 3. Arquitectura

```
┌─────────────────────────── Java (sin cambios de lógica) ───────────────────────────┐
│ 1. Resuelve headers, aplica regex, valida/convierte tipos  (prepareRowData/convert) │
│ 2. Calcula existing_id (identity→nombre)  [solo upsert]                              │
│ 3. Filas inválidas → contador `failed` (NO van a staging)                           │
│ 4. CREATE staging (1 por carga, nombre con id_sesion)                               │
│ 5. batchUpdate inserta filas válidas (lotes 1–5k)                                    │
└──────────────────────────────────────────────┬──────────────────────────────────────┘
                                                │  CALL sp_import_*(...)
┌──────────────────────────────────────────────▼──────────────────────────────────────┐
│ Stored Procedure (SQL dinámico, set-based)                                            │
│  - Introspecta columnas de staging vía information_schema (excluye columnas control)  │
│  - Construye y EXECUTE: UPDATE…JOIN / INSERT…SELECT contra la tabla dinámica destino  │
│  - Devuelve OUT: insertados, actualizados, no_encontrados                             │
└──────────────────────────────────────────────┬──────────────────────────────────────┘
                                                │
┌──────────────────────────────────────────────▼──────────────────────────────────────┐
│ Java: lee OUT params, DROP staging, arma Map de respuesta (claves idénticas), corre  │
│       la sincronización de clientes igual que hoy.                                    │
└───────────────────────────────────────────────────────────────────────────────────────┘
```

### 3.1 Staging: tabla horizontal por carga (introspección)

Se elige staging **horizontal con columnas tipadas reales** (no EAV) para que el SP genere **una sola sentencia set-based con tipos nativos** (sin `CAST`). Como MySQL **no expone `TEMPORARY TABLE` en `information_schema`**, la staging es **persistente** con nombre único por sesión y se dropea al final (mismo patrón que `staging_carga_telefonos`).

**Nombre:** `cashi_discador_db.stg_<dest_table>_<id_sesion8>` (id_sesion = UUID corto generado en Java).

**Columnas de control (prefijo `__` para excluirlas en la introspección):**
| Columna | Tipo | Uso |
|---|---|---|
| `__row` | INT | nº de fila original (trazabilidad de errores) |
| `__existing_id` | BIGINT NULL | id destino precomputado (solo upsert) |
| `__link` | VARCHAR(255) NULL | valor del link field (solo update) |

**Columnas de datos:** una por cada columna destino a escribir, con el **tipo SQL exacto** de la tabla destino (vía `mapDataTypeToSQL`, ya existente). Java inserta los valores ya convertidos con su tipo JDBC → fidelidad total.

**DDL (generado en Java, una vez por carga):**
```sql
CREATE TABLE cashi_discador_db.stg_<dest>_<sid> (
  __row         INT NOT NULL,
  __existing_id BIGINT NULL,           -- solo upsert
  __link        VARCHAR(255) NULL,     -- solo update
  <col1> <tipo1>, <col2> <tipo2>, ...  -- mismas columnas/tipos que destino
  KEY idx_existing (__existing_id),
  KEY idx_link (__link)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

> **Colación (CRÍTICO — validado en QAS):** las tablas dinámicas **NO tienen colación homogénea** (`foh_cas_cst`=`utf8mb4_unicode_ci`, `ini_foh_cas_cst`=`utf8mb4_0900_ai_ci`). Por eso la staging **NO** puede hardcodear una colación: debe **leer `table_collation` del destino vía `information_schema` y crear las columnas VARCHAR de staging con esa MISMA colación**. Así el JOIN `staging.__link = dest.<link_col>` no lanza *"Illegal mix of collations"*. La staging se crea en `cashi_db` (misma base que el destino) para evitar JOIN cross-DB.

---

## 4. Stored Procedures

### 4.1 `sp_import_upsert` — INICIAL y ACTUALIZACION (Fase 1 diaria)

```
sp_import_upsert(
  IN  p_staging   VARCHAR(128),   -- nombre tabla staging
  IN  p_dest      VARCHAR(128),   -- tabla dinámica destino
  OUT p_inserted  INT,
  OUT p_updated   INT
)
```

Pseudo-lógica (SQL dinámico):
```sql
-- 1. Lista de columnas de datos = columnas de staging que NO empiezan por '__'
SELECT GROUP_CONCAT(column_name) INTO @cols
FROM information_schema.columns
WHERE table_schema='cashi_discador_db' AND table_name=p_staging
  AND column_name NOT LIKE '\_\_%';

-- 2. UPDATE de existentes (existing_id no nulo)
SET @set = <"d.colX = s.colX" por cada col en @cols>;
SET @sql = CONCAT('UPDATE ',p_dest,' d JOIN ',p_staging,' s ',
                  'ON d.id = s.__existing_id SET ',@set,' WHERE s.__existing_id IS NOT NULL');
PREPARE st FROM @sql; EXECUTE st; SET p_updated = ROW_COUNT(); DEALLOCATE PREPARE st;

-- 3. INSERT de nuevos (existing_id nulo)
SET @sql = CONCAT('INSERT INTO ',p_dest,' (',@cols,') ',
                  'SELECT ',@cols,' FROM ',p_staging,' WHERE __existing_id IS NULL');
PREPARE st FROM @sql; EXECUTE st; SET p_inserted = ROW_COUNT(); DEALLOCATE PREPARE st;
```

Equivalencia con hoy: `insertedRows = p_inserted`, `updatedRows = p_updated`, `failedRows` lo aporta Java (filas rechazadas en validación). El matching identity→nombre se preserva porque `__existing_id` lo decidió Java.

### 4.2 `sp_import_update` — Complementary y Fase 2 diaria

```
sp_import_update(
  IN  p_staging      VARCHAR(128),
  IN  p_dest         VARCHAR(128),
  IN  p_link_col     VARCHAR(128),   -- columna destino para el JOIN
  IN  p_use_coalesce TINYINT,        -- 1 = COALESCE(s.col, d.col) [diaria]; 0 = s.col [complementary]
  OUT p_updated      INT,
  OUT p_not_found    INT
)
```

Pseudo-lógica:
```sql
-- columnas de datos (excluye '__%')
SELECT GROUP_CONCAT(column_name) INTO @cols ... ;

-- SET clause según flag
--   coalesce=1 → d.col = COALESCE(s.col, d.col)
--   coalesce=0 → d.col = s.col
SET @set = <según p_use_coalesce>;

SET @sql = CONCAT('UPDATE ',p_dest,' d JOIN ',p_staging,' s ',
                  'ON d.',p_link_col,' = s.__link SET ',@set);
PREPARE st FROM @sql; EXECUTE st; SET p_updated = ROW_COUNT(); DEALLOCATE PREPARE st;

-- no encontrados = filas de staging sin match en destino
SET @sql = CONCAT('SELECT COUNT(*) FROM ',p_staging,' s LEFT JOIN ',p_dest,' d ',
                  'ON d.',p_link_col,' = s.__link WHERE d.id IS NULL');
PREPARE st FROM @sql; EXECUTE st INTO @nf; SET p_not_found = @nf; DEALLOCATE PREPARE st;
```

> **Nota `updated` vs `ROW_COUNT()`**: MySQL cuenta como "changed rows" solo las que realmente cambiaron de valor. Hoy Java cuenta `rowsAffected > 0`. Para igualar el conteo actual usar `useAffectedRows=true` (flag de conexión `CLIENT_FOUND_ROWS`) **o** medir con un `JOIN COUNT` previo. Ver §8 (riesgo R3).

> **Códigos actualizados para sync selectivo (diaria)**: tras el UPDATE, Java obtiene los `__link` que matchearon con
> `SELECT s.__link FROM stg s JOIN dest d ON d.<link_col>=s.__link` para alimentar `syncCustomersByIdentificationCodes`.

---

## 5. Índices y rendimiento (validado en QAS)

- `UPDATE…JOIN ON d.id = s.__existing_id`: usa PK `id INT AUTO_INCREMENT` del destino → óptimo. (Confirmado: todas las tablas tienen PK `id`.)
- `UPDATE…JOIN ON d.<link_col> = s.__link`: requiere **índice en `<link_col>`** del destino.
  - Las tablas **INICIAL maestras** (sin prefijo, ej. `foh_cas_cst`) traen auto-índice `idx_<tabla>_identity_code` (lo crea el código al crear la tabla, `:376`). Si el linkField **es** `identity_code` → indexado.
  - **Si el linkField es otro** (documento, num_cuenta, etc.) → **NO hay índice** → full scan por fila (problema que **ya existe hoy** y degrada la carga). La migración debe **crear el índice en `<link_col>` si falta** (beneficia también al motor legacy).
  - Las tablas **ACTUALIZACION** (prefijo `ini_`) tienen **solo PK `id`**, sin índices secundarios (verificado).
- Staging indexada en `__existing_id` y `__link`.

> **Acción de migración:** antes del `UPDATE…JOIN`, garantizar `CREATE INDEX IF (not exists) ON <dest>(<link_col>)`. MySQL no soporta `IF NOT EXISTS` en `CREATE INDEX`; verificar contra `information_schema.statistics` primero.

---

## 6. Map de respuesta — claves EXACTAS a preservar

El frontend (`consolidated-load.component.ts`) lee:

**Diaria** (`executeDailyLoad`): `result.actualizacion.insertedRows`, `result.actualizacion.updatedRows`, `result.actualizacion.failedRows`, `result.inicial.updatedRows`, `result.inicial.failedRows`, `result.syncCustomersCreated`, `result.syncCustomersUpdated`, `result.errors`.

**Inicial** (`executeInitialMonthLoad`): `mainResult.insertedRows`, `mainResult.failedRows`, + resultados complementarios.

**Complementary** (`processComplementaryFiles`): `result.updatedRows`, `result.failedRows`.

→ El armado del `Map<String,Object>` se mantiene **textualmente igual**; solo cambia de dónde salen los números (`OUT` del SP en lugar de contadores del bucle).

---

## 7. Conteos: de dónde sale cada número

| Conteo | Fuente nueva |
|---|---|
| `failedRows` | Java (validación previa, antes de staging) — **igual que hoy** |
| `insertedRows` | `sp_import_upsert.p_inserted` |
| `updatedRows` (upsert) | `sp_import_upsert.p_updated` |
| `updatedRows` (update) | `sp_import_update.p_updated` |
| `notFoundRows` | `sp_import_update.p_not_found` |
| `errors[]` | Java (mensajes por fila inválida) — **igual que hoy** |

---

## 8. Riesgos y mitigaciones

| ID | Riesgo | Mitigación |
|---|---|---|
| R1 | Colación cross-DB rompe JOIN silenciosamente | `utf8mb4_unicode_ci` explícito en staging + `COLLATE` en JOIN si hace falta (precedente teléfonos) |
| R2 | `TEMPORARY TABLE` no visible en `information_schema` → introspección falla | Staging **persistente** con nombre por sesión + `DROP` en `finally` |
| R3 | `ROW_COUNT()` cuenta "changed" no "matched" → `updatedRows` difiere del actual | Activar `CLIENT_FOUND_ROWS` o contar matches con JOIN; validar en parallel-run |
| R4 | DDL `CREATE/DROP` por carga (coste + permisos) | Aceptable (1 por carga); el SP de teléfonos ya lo asume. Limpieza defensiva de staging huérfana > 1 día |
| R5 | Cargas concurrentes misma subcartera | Nombre de staging incluye `id_sesion` único → sin colisión |
| R6 | Inyección vía nombres de columna/tabla en SQL dinámico | Solo se concatenan nombres ya sanitizados por `SqlSanitizer`; validar contra `information_schema` antes de EXECUTE |
| R7 | Atomicidad SP vs sync de clientes | SP y sync siguen en la misma `@Transactional` del método (sin cambios de frontera) |

---

## 9. Feature flag + parallel-run

### 9.1 Flag
Propiedad `app.import.engine=legacy|sp` (default `legacy`). En cada método:
```java
if (importProps.isSpEngine()) return importDataViaSp(...);
else                          return importDataLegacy(...);   // código actual intacto
```
El código legacy **no se borra** hasta validar; rollback = cambiar la propiedad.

### 9.2 Parallel-run de equivalencia (test)
Test de integración que, sobre el **mismo dataset** y una **tabla destino espejo**:
1. Corre `legacy` sobre `dest_A`, `sp` sobre `dest_B` (copias idénticas).
2. Compara: (a) `SELECT *` ordenado de ambas tablas fila-a-fila; (b) los `Map` de respuesta (mismas claves y números); (c) efectos en `clientes`/`metodos_contacto`.
3. Falla el test ante cualquier diferencia.
Datasets: casos reales (INNOVAG) + edge cases (regex, fechas, obligatorios vacíos, identity vs nombre, link no encontrado, duplicados, NULLs/COALESCE).

---

## 10. Migración del SP (Flyway vs directo)

Los SP de este servicio viven en `src/main/resources/db/migration` (V13/V14). **Pero** el precedente de teléfonos indica *"EJECUTAR DIRECTO EN BD (no vía Flyway)"* por temas de colación cross-DB.
**Propuesta:** crear `V<next>__create_sp_import.sql` idempotente (`DROP PROCEDURE IF EXISTS` + `CREATE`) y decidir en deploy si va por Flyway o ejecución manual documentada. Confirmar el siguiente número de versión libre antes de crearlo.

---

## 11. Fases de entrega

| Fase | Entregable | Archivos |
|---|---|---|
| **F0** | Infra staging + 2 SP + flag (apagado) | `V<n>__create_sp_import.sql`, `ImportEngineProperties`, helpers staging en el service |
| **F1** | `importDataToTable` vía SP (INICIAL + Fase 1 diaria) + parallel-run | `HeaderConfigurationCommandServiceImpl` |
| **F2** | Fase 2 diaria + `updateComplementaryDataInTable` vía SP | idem |
| **F3** | Activación gradual por subcartera + monitoreo de tiempos | config |
| **F4** (opcional) | SP de `CustomerSyncService` (atomicidad) | nuevo SP |

---

## 12. Checklist "no se chanca nada"

- [ ] Transformaciones (regex/tipos/obligatorios) intactas en Java.
- [ ] `existing_id` (identity→nombre) calculado en Java igual que hoy.
- [ ] COALESCE preservado en update diaria; NO en complementary.
- [ ] Claves del Map de respuesta idénticas (§6).
- [ ] `updatedRows` valida igual conteo que legacy (R3).
- [ ] Sync de clientes (full vs selectiva) + reapply inactivación sin cambios.
- [ ] Colación e índices verificados.
- [ ] Parallel-run verde sobre dataset real + edge cases.
- [ ] Flag permite rollback inmediato.

---

## 13. Validación contra esquema real (QAS `18.188.41.174`, MySQL 8.0.46)

Verificado directamente en la BD de QAS (usuario `admin`):

### 13.1 Bases de datos
| Base | Colación default | Rol |
|---|---|---|
| `cashi_db` | `utf8mb4_unicode_ci` | **MAIN** — tablas dinámicas + `clientes`, `metodos_contacto` |
| `cashi_discador_db` | `utf8mb4_unicode_ci` | discador (no se tocan dinámicas aquí) |
| `cashi_historico_db` | `utf8mb4_0900_ai_ci` | archivos de snapshot (`<tabla>_yyyy_mm`) |

### 13.2 Convención de nombres (¡contraintuitiva! — confirmada por código y datos)
`LoadType.getTablePrefix()` (`LoadType.java:13-20`) + comentario `buildTableName:534`:
- **INICIAL** (tabla maestra de trabajo) → **prefijo vacío**: `foh_cas_cst`, `foh_nso_tpr`, `sam_mas_elm`.
- **ACTUALIZACION** (histórico diario) → **prefijo `ini_`**: `ini_foh_cas_cst`, `ini_foh_nso_tpr`.

> El SP recibe el **nombre completo de tabla desde Java** (`buildTableName`), nunca infiere prefijos → inmune a esta convención.

### 13.3 Estructura de tabla dinámica (ej. maestra `foh_cas_cst`, 4305 filas)
- **PK `id` INT AUTO_INCREMENT** ✓ (válido para `__existing_id`).
- Columnas tipadas: `decimal(18,2)`, `varchar(255)`, `date` (sin `NOT NULL` salvo `id`).
- **Índices**: `PRIMARY(id)` + `idx_foh_cas_cst_identity_code(identity_code)` (auto, no único).
- Colación tabla = `utf8mb4_unicode_ci`; `identity_code` y `documento` = `utf8mb4_unicode_ci`.
- Datos: 4461/4461 `identity_code` distintos, 0 vacíos (limpio en esta subcartera).

### 13.4 Heterogeneidad de colación (riesgo R1 CONFIRMADO)
- `foh_cas_cst` → `utf8mb4_unicode_ci`
- `ini_foh_cas_cst` → `utf8mb4_0900_ai_ci`
- `ini_foh_nso_tpr` → `utf8mb4_unicode_ci`
→ La staging **debe heredar la colación del destino dinámicamente** (ver §3.1).

### 13.5 Índices faltantes (riesgo de rendimiento CONFIRMADO)
- Tablas **ACTUALIZACION** (`ini_*`): solo `PRIMARY(id)`, sin índices secundarios.
- Tablas **INICIAL** (`foh_*`): índice solo en `identity_code`. Si el linkField es otro → full scan (hoy y con SP) → la migración debe crear el índice (§5).

### 13.6 Sincronización de clientes
- `clientes` (21073 filas) y `metodos_contacto` (168616 filas) en `cashi_db`, `utf8mb4_unicode_ci`.
- Su colación difiere de algunas tablas dinámicas (`0900_ai_ci`) → **se mantiene el matching en Java** (`CustomerSyncService`, ya batched) para evitar JOIN cross-colación. Sin cambios en esta iteración.

### 13.7 No existe clave única para UPSERT nativo
Las tablas dinámicas **no** tienen UNIQUE en `identity_code` (solo índice no único) → no se puede usar `INSERT … ON DUPLICATE KEY UPDATE`. **Confirma** que el matching insert-vs-update debe seguir en Java (vía `__existing_id`), como define el diseño.

---

## 14. Auditoría de integridad del SYNC (incongruencias que "escapan de la realidad")

> Disparador: tras `UPDATE metodos_contacto SET estado_osiptel='PERTENECE'` + una carga diaria, reaparecieron 13.421 `SIN_VALIDAR`. La causa es estructural y genera **varias** incongruencias, no una.

### 14.1 Causa raíz común: el sync de contactos es DELETE + INSERT, no UPSERT
`CustomerSyncService.syncAllCustomerContactsBatch` (lo invocan la **carga inicial** —sync total— y la **diaria** —sync selectivo—):
- **DELETE** (`:1055`) de **todos** los contactos del cliente salvo los etiquetados `'Agregado por agente'` (solo 294 filas).
- **INSERT** (`:1108-1110`) con **literales fijos**: `estado='ACTIVE', estado_osiptel='SIN_VALIDAR', estado_whatsapp='SIN_VALIDAR', estado_contactabilidad='NUEVO', fecha_importacion=CURDATE()`.

### 14.2 Incongruencias confirmadas (evidencia en QAS)

| # | Incongruencia | Evidencia en vivo | Severidad |
|---|---|---|---|
| I1 | `estado_osiptel` se resetea a `SIN_VALIDAR` en cada sync | 13.778 `NO_PERTENECE` + 146.834 `PERTENECE` en riesgo; 13.421 ya reseteados | 🔴 Alta |
| I2 | `estado_whatsapp` se resetea igual | 1.199 `TIENE` + 2.340 `NO_TIENE` en riesgo (170.494 `SIN_VALIDAR`) | 🔴 Alta |
| I3 | `estado_contactabilidad` → `NUEVO` en cada sync | toda la base | 🟠 Media |
| I4 | `estado` → `ACTIVE`: **reactiva teléfonos inactivados** | 1.964 `INACTIVE` se reactivan; el SP de limpieza (§14.3) solo recupera FALLECIDO/EQUIVOCADO, **no** OSIPTEL `NO_PERTENECE` ni WhatsApp `NO_TIENE` | 🔴 Alta |
| I5 | `fecha_importacion` → `CURDATE()`: se pierde la fecha real | toda la base | 🟡 Baja |
| I6 | **Churn de `id`**: cada contacto se recrea con id nuevo | rompe FKs hacia `metodos_contacto.id` (existe `scoring_metodos_contacto`) | 🟠 Media |
| I7 | **Pérdida de `telefono_extra`**: el DELETE los borra pero el INSERT solo restaura 6 subtipos estándar | 107.691 filas `telefono_extra` en riesgo si su cliente entra a un sync | 🔴 Alta (verificar) |
| I8 | Clientes sin `codigo_identificacion` (NULL) no upsertean bien (UNIQUE permite multi-NULL) | 16 clientes con código vacío → posibles duplicados / contactos no sincronizados | 🟡 Baja |
| I9 | Matching UPSERT por **nombre** (fallback de `importDataToTable` cuando `identity_code` vacío): homónimos se pisan | solo 6 homónimos hoy → riesgo bajo pero real | 🟡 Baja |

### 14.3 Por qué I4 es real
`reapplyInvalidPhoneContactInactivation` → SP `sp_limpiar_contactos_invalidos` (V22) **solo** re-inactiva por gestión:
```sql
... SET m.estado='INACTIVE' WHERE g.ruta_nivel_2 IN ('FALLECIDO','EQUIVOCADO') ...
```
No considera `estado_osiptel='NO_PERTENECE'` ni `estado_whatsapp='NO_TIENE'`. Por eso un teléfono que OSIPTEL marcó como NO_PERTENECE vuelve a `ACTIVE`+`SIN_VALIDAR` y se vuelve a discar/validar.

### 14.4 Solución íntegra: SYNC NO DESTRUCTIVO (reconciliación)
Reemplazar DELETE+INSERT por **reconciliación por clave de negocio** `(id_cliente, tipo_contacto, valor)`:

| Caso | Acción nueva | Preserva |
|---|---|---|
| Contacto existe y sigue en archivo | **UPDATE** solo `etiqueta`/`subtipo` (o nada) | `id`, `estado`, `estado_osiptel`, `estado_whatsapp`, `estado_contactabilidad`, `fecha_importacion` |
| En archivo, no existe | **INSERT** con defaults `SIN_VALIDAR`/`NUEVO` | — |
| Existe pero ya no está en archivo | **soft-deactivate** (`estado='INACTIVE'`, motivo) — **nunca** borrar | manuales y `telefono_extra` intactos |

Esto **mata de raíz I1–I7** (estados y `id` se preservan; los extras no se borran).

**Prerrequisito estructural:** agregar `UNIQUE (id_cliente, tipo_contacto, valor)` en `metodos_contacto` (hoy no existe → §13 metodos_contacto solo PK + índice no único). Antes, **deduplicar** filas repetidas existentes. Con el UNIQUE, la reconciliación es:
```sql
INSERT INTO metodos_contacto (... , estado, estado_osiptel, estado_whatsapp, estado_contactabilidad)
VALUES (..., 'ACTIVE','SIN_VALIDAR','SIN_VALIDAR','NUEVO')
ON DUPLICATE KEY UPDATE etiqueta=VALUES(etiqueta), subtipo=VALUES(subtipo);
   -- NO toca los estado_* en filas existentes
```
Encaja con la decisión **SP-puro**: se implementa como `sp_sync_contactos(staging, ...)` alimentado por staging (mismo patrón del resto del plan), con la desactivación de ausentes en una sentencia set-based aparte.

### 14.5 Incongruencias del lado de carga/periodo (del audit previo, a incluir)
- **P1** Snapshot mensual archiva con `YearMonth.now()` y no con el periodo de los datos → etiqueta histórica errónea si el cierre se hace el mes siguiente.
- **P2** "Cambio de periodo" se dispara con *cualquier* dato existente (no cambio real) → re-archivado a la misma tabla `_yyyy_MM` (colisión) en segundas cargas del mismo mes.

---

## 15. Impacto en las fases del plan (actualizado)

| Fase | Entregable | Estado |
|---|---|---|
| **F2.5-a — Fix quirúrgico (HECHO)** | En `syncAllCustomerContactsBatch`: (1) DELETE acotado a los 6 subtipos gestionados → preserva `telefono_extra` (I7); (2) precarga de estados (`loadExistingContactStates`) + INSERT parametrizado que **preserva** `estado/estado_osiptel/estado_whatsapp/estado_contactabilidad/fecha_importacion` (I1–I5). Compila OK. **Pendiente: probar en QAS.** | ✅ implementado |
| **F0 — SP de import** | `sp_import_upsert` y `sp_import_update` en `V23__create_sp_import.sql`, **verificados contra `foh_cas_cst` real en QAS** y **creados en la BD QAS** (Flyway inactivo → ejecutados a mano). Fix `group_concat_max_len`. | ✅ hecho |
| **F1 — Wiring Java upsert (HECHO)** | `importDataToTableViaSp` detrás del flag `app.import.engine` (default `legacy`). Crea staging heredando **colación y tipos reales del destino**, puebla con `batchUpdate`, hace `CALL sp_import_upsert`, lee OUT params, dropea staging, arma el Map con **claves idénticas** y corre el sync igual que legacy. Cubre INICIAL y Fase 1 diaria. Compila OK. | ✅ implementado (flag off) |
| **F2 — Wiring update (HECHO)** | Fase 2 diaria (COALESCE) y `updateComplementaryDataInTable` (sin COALESCE) conectadas a `sp_import_update` vía helper compartido `runImportUpdateViaSp` (staging hereda colación, `batchUpdate`, `CALL`, OUT params, drop). El helper devuelve también los `__link` que matchearon → alimenta el **sync selectivo** de la diaria. Compila OK. Con el flag en `sp`, los 3 métodos de escritura usan SP. | ✅ implementado (flag off) |

**Divergencia intencional (correcta) en F2:** `notFoundRows` se cuenta con un `LEFT JOIN` real (link ausente en destino), no con `ROW_COUNT()==0` como el legacy (que contaba como "no encontrado" también las filas existentes sin cambios). El motor SP es más preciso → en el parallel-run `notFoundRows`/`updatedRows` pueden diferir del legacy **por corrección**, no por error.

### Divergencias esperadas SP vs legacy (parallel-run con criterio, NO igualdad estricta)
Por indicación: el legacy es deficiente en partes; el motor SP debe ser **correcto**, no bug-compatible. Para `importDataToTableViaSp` específicamente:
- **Equivalencia esperada**: la preparación (regex/tipos/obligatorios/matching identity→nombre) y el sync son idénticos al legacy → para `importDataToTable` el resultado en la tabla destino y las claves del Map **deben coincidir**. Aquí la igualdad sí es buena señal.
- **Fuera de alcance de este método** (donde legacy es deficiente y NO se debe exigir igualdad): el reseteo de `estado_*` del sync (ya corregido en F2.5-a) y la lógica de periodo del snapshot (P1/P2). El parallel-run debe juzgar esos por correctitud, no por paridad con legacy.

### Caveat de atomicidad (F1)
`CREATE`/`DROP` de la staging son DDL → **commit implícito**, por lo que `importDataToTableViaSp` no es transaccional como el legacy. Endurecer antes de habilitar en producción (p. ej. transacción interna en el SP alrededor de UPDATE+INSERT, o staging pre-creada por sesión). Mientras tanto el flag queda en `legacy`.
| **F2.5-b — Estructural (recomendado)** | UNIQUE `(id_cliente,tipo_contacto,valor)` + reconciliación real (upsert sin DELETE, soft-deactivate de ausentes) → elimina además el **churn de `id` (I6)**. **Dedup previo dimensionado: solo 561 filas sobrantes en 547 grupos, 0 con `estado_osiptel` en conflicto** → limpieza trivial y sin pérdida. | pendiente |
| F3 | Activación gradual + parallel-run (comparar también que `estado_*` NO cambian en recargas) | pendiente |
| F4 | (opcional) SP de sync de `clientes` (atomicidad) | pendiente |
| **F5 (baja prioridad)** | Arreglar P1/P2 del snapshot (periodo de archivado real, distinguir cambio de periodo) | pendiente |

> **Nota:** el fix F2.5-a detiene el reseteo hacia adelante. Las ~13.421 filas ya reseteadas a `SIN_VALIDAR` por cargas previas **no se auto-recuperan**: requieren re-validación OSIPTEL/WhatsApp o una corrección puntual de datos (no se hace automáticamente).
