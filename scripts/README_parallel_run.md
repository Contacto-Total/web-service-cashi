# Parallel-run harness — Carga Consolidada (motor legacy vs SP)

Valida el motor de importación **activo** en el servicio vivo por **aserciones de
correctitud**, no por paridad con legacy (el legacy es deficiente en partes; el SP
debe ser *correcto*). Es **reversible**: captura el estado real, llama a la API,
verifica y restaura.

## Por qué así
El flag `app.import.engine` se lee al **arranque** (no se conmuta en caliente), así que
el parallel-run son **dos corridas** del mismo harness sobre el mismo dataset:

1. Deploy con `app.import.engine=legacy` → correr el harness → **baseline** (debe PASAR).
2. Redeploy con `app.import.engine=sp` → correr el harness → debe **PASAR igual** (y más rápido).

Si la corrida `sp` falla una aserción que `legacy` pasó, hay un bug en el wiring SP.

> Nota: el script usa `update-complementary` porque ejercita `sp_import_update`
> (COALESCE/colación/tipos) y **no dispara** la sincronización de clientes → impacto mínimo.
> Para validar `import-data` (upsert + sync) e `import-daily`, replicar el patrón sobre una
> **subcartera desechable** (los mutan sync/clientes).

## Requisitos (en el host del servicio)
- `curl` y cliente `mysql` (el host ya tiene la BD).
- El servicio escuchando en `localhost:8085` (sin auth a nivel servicio).

## Uso
```bash
cd scripts
chmod +x parallel_run_carga.sh
DBPASS='<password_admin>' ./parallel_run_carga.sh
```
Variables override: `BASE_URL SUB TABLE COL LINK DBHOST DBUSER DBPASS DB`.
Default: `SUB=27` (foh_cas_cst), `COL=score_final`, `LINK=identity_code`.

## Qué valida
- El UPDATE por link field llega a las filas correctas (3 `identity_code` reales).
- Respuesta esperada: `updatedRows=3`, `notFoundRows=1` (incluye un link inexistente).
  - **Divergencia intencional vs legacy**: `notFoundRows` del SP usa LEFT JOIN real
    (link ausente), mientras el legacy contaba `ROW_COUNT()==0` (incluía filas sin cambio).
    El SP es más correcto.
- Restaura los valores originales al terminar.

## Validación a nivel DB ya realizada (sin servicio)
Contra datos reales en QAS, de forma reversible (rollback):
- `sp_import_upsert`: insert/update + carga parcial de columnas. ✓
- `sp_import_update`: COALESCE preserva NULL, conteo not_found. ✓
- Colación heredada: join OK en tabla `0900_ai_ci`; colación incorrecta → `ERROR 1267`. ✓
