# Sistema de Carga de Cartera y Visualización de Clientes

## 📋 Objetivo

Implementar un sistema completo que permita:
1. **Cargar carteras** desde Excel/CSV con formato flexible por tenant
2. **Visualizar datos del cliente** en la pantalla de gestión
3. **Simular gestiones reales** con datos completos del cliente
4. **Mapeo dinámico** de columnas según el formato de cada financiera

## ✅ Completado

### 1. Entidad Customer (Cliente)
**Archivo**: `Customer.java`
**Ubicación**: `com.cashi.collectionmanagement.domain.model.aggregates`

**Campos principales (todos en español)**:
- Información básica: `codigo_documento`, `nombre_completo`, `nombres`, `apellido_paterno`
- Contacto: `telefono_celular`, `telefono_domicilio`, `telefono_laboral`, `telefono_referencia_1/2`
- Dirección: `direccion`, `distrito`, `provincia`, `departamento`
- Cuenta: `numero_cuenta`, `deuda_actual`, `saldo_capital`, `saldo_mora`, `dias_mora`
- Estado: `estado`, `estado_asignacion`, `esta_bloqueado`, `periodo_asignacion`
- Metadata: `fecha_importacion`, `fecha_ultimo_contacto`, `fecha_ultimo_pago`
- **Flexible**: `datos_adicionales_json` para campos custom por tenant

## 📝 Pendiente de Implementar

### 2. Repository

```java
// CustomerRepository.java
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCodigoDocumentoAndTenant(String codigoDocumento, Tenant tenant);

    Optional<Customer> findByNumeroCuentaAndTenant(String numeroCuenta, Tenant tenant);

    List<Customer> findByTenantAndEstado(Tenant tenant, String estado);

    List<Customer> findByTenantAndCampaign(Tenant tenant, Campaign campaign);

    List<Customer> findByTenantAndLoteImportacion(Tenant tenant, String loteImportacion);

    @Query("SELECT c FROM Customer c WHERE c.tenant = :tenant AND c.diasMora >= :minDias")
    List<Customer> findByTenantAndMinDayOverdue(Tenant tenant, Integer minDias);
}
```

### 3. Configuración de Mapeo por Tenant

**Archivo JSON**: `tenant-configurations/financiera-oh-mapping.json`

```json
{
  "tenantCode": "FIN-OH",
  "sourceType": "ASIG_TEMP",
  "description": "Mapeo de tabla ASIG_TEMP a entidad Customer",
  "columnMappings": {
    "PERIODO": "periodoAsignacion",
    "ESTUDIO": "estudio",
    "TIPO": "tipoAsignacion",
    "IDENTITY_CODE": "codigoDocumento",
    "NUM_CUENTA_PMCP": "numeroCuenta",
    "DIAS_MORA_ASIG": "diasMora",
    "RANGO_MORA_PROY_AG": "rangoMora",
    "SLD_MORA_ASIG": "saldoMora",
    "SLD_CAPITAL_ASIG": "saldoCapital",
    "SLD_TOTAL_ASIG": "deudaActual",
    "ENTIDAD": "nombreEntidad",
    "DIAS_MORA": "diasMora",
    "SLD_MORA": "saldoMora",
    "SLD_CAP_CONS": "saldoCapital",
    "SLD_ACTUAL_CONS": "deudaActual",
    "FEC_VENCIMIENTO": "fechaVencimiento",
    "NOMBRE_COMPLETO": "nombreCompleto",
    "NOMBRE": "nombres",
    "APELLIDO_PATERNO": "apellidoPaterno",
    "TELEFONO_CELULAR": "telefonoCelular",
    "TELEFONO_DOMICILIO": "telefonoDomicilio",
    "TELEFONO_LABORAL": "telefonoLaboral",
    "TELF_REFERENCIA_1": "telefonoReferencia1",
    "TELF_REFERENCIA_2": "telefonoReferencia2",
    "EDAD": "edad",
    "EMAIL": "correoElectronico",
    "FLAG_DEPENDIENTE": "esDependiente",
    "DIRECCION": "direccion",
    "DISTRITO": "distrito",
    "DEPARTAMENTO": "departamento",
    "LTD_ESPECIAL": "limiteEspecial",
    "BLOQUEO": "estaBloqueado"
  },
  "transformations": {
    "FLAG_DEPENDIENTE": {
      "type": "boolean",
      "trueValues": ["SI", "S", "1", "TRUE"],
      "falseValues": ["NO", "N", "0", "FALSE"]
    },
    "BLOQUEO": {
      "type": "boolean",
      "trueValues": ["S", "SI", "1"],
      "falseValues": ["N", "NO", "0", ""]
    },
    "FEC_VENCIMIENTO": {
      "type": "date",
      "format": "yyyy/MM/dd"
    },
    "SLD_MORA_ASIG": {
      "type": "decimal"
    },
    "SLD_CAPITAL_ASIG": {
      "type": "decimal"
    },
    "SLD_TOTAL_ASIG": {
      "type": "decimal"
    }
  },
  "businessRules": {
    "setDefaultStatus": "ACTIVO",
    "setDefaultState": "ASIGNABLE",
    "calculateAge": true,
    "validateDocument": true,
    "deduplicateBy": ["codigoDocumento", "numeroCuenta"]
  }
}
```

### 4. Servicio de Importación

```java
// CustomerImportService.java
@Service
public class CustomerImportService {

    private final CustomerRepository customerRepository;
    private final TenantRepository tenantRepository;
    private final ObjectMapper objectMapper;

    /**
     * Importa clientes desde un archivo Excel/CSV
     */
    public ImportResult importCustomersFromFile(MultipartFile file, String tenantCode, String batchId) {
        // 1. Leer configuración de mapeo del tenant
        // 2. Leer archivo Excel/CSV
        // 3. Mapear columnas según configuración
        // 4. Aplicar transformaciones
        // 5. Validar datos
        // 6. Guardar o actualizar clientes
        // 7. Retornar resultado con estadísticas
    }

    /**
     * Importa desde tabla temporal ASIG_TEMP
     */
    public ImportResult importFromAsigTemp(String tenantCode, String periodo) {
        // Query directo a ASIG_TEMP
        // Mapear según configuración
        // Guardar clientes
    }
}
```

### 5. Endpoint REST para Importación

```java
// CustomerController.java
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    @PostMapping("/import")
    public ResponseEntity<ImportResultResource> importCustomers(
        @RequestParam("file") MultipartFile file,
        @RequestParam("tenantCode") String tenantCode,
        @RequestParam(value = "batchId", required = false) String batchId
    ) {
        // Llamar servicio de importación
        // Retornar resultado
    }

    @PostMapping("/import/asig-temp")
    public ResponseEntity<ImportResultResource> importFromAsigTemp(
        @RequestParam("tenantCode") String tenantCode,
        @RequestParam("periodo") String periodo
    ) {
        // Importar desde tabla temporal
    }

    @GetMapping("/{documento}")
    public ResponseEntity<CustomerResource> getCustomerByDocument(
        @PathVariable String documento,
        @RequestHeader("X-Tenant-Code") String tenantCode
    ) {
        // Obtener cliente por documento
    }

    @GetMapping("/account/{numeroCuenta}")
    public ResponseEntity<CustomerResource> getCustomerByAccount(
        @PathVariable String numeroCuenta,
        @RequestHeader("X-Tenant-Code") String tenantCode
    ) {
        // Obtener cliente por número de cuenta
    }
}
```

### 6. DTO/Resource para Cliente

```java
// CustomerResource.java
public record CustomerResource(
    Long id,
    String codigoDocumento,
    String tipoDocumento,
    String nombreCompleto,
    String nombres,
    String apellidoPaterno,
    String correoElectronico,
    Integer edad,

    // Contacto
    String telefonoCelular,
    String telefonoDomicilio,
    String telefonoLaboral,
    String telefonoReferencia1,
    String telefonoReferencia2,

    // Dirección
    String direccion,
    String distrito,
    String provincia,
    String departamento,

    // Cuenta
    String numeroCuenta,
    String nombreEntidad,
    BigDecimal deudaActual,
    BigDecimal saldoCapital,
    BigDecimal saldoMora,
    Integer diasMora,
    String rangoMora,
    LocalDate fechaVencimiento,

    // Estado
    String estado,
    String estadoAsignacion,
    Boolean estaBloqueado,
    String periodoAsignacion,

    // Metadata
    LocalDate fechaImportacion,
    LocalDate fechaUltimoContacto,
    LocalDate fechaUltimoPago,

    // Adicionales
    Map<String, Object> datosAdicionales
) {}
```

### 7. Frontend - Actualizar CustomerData

**Actualizar**: `collection-management.page.ts`

```typescript
export interface CustomerData {
  // Básico
  id_cliente: string;
  codigo_documento: string;
  tipo_documento?: string;
  nombre_completo: string;
  nombres?: string;
  apellido_paterno?: string;
  apellido_materno?: string;
  correo_electronico?: string;
  edad?: number;

  // Contacto
  telefono_celular?: string;
  telefono_domicilio?: string;
  telefono_laboral?: string;
  telefono_referencia_1?: string;
  telefono_referencia_2?: string;

  // Dirección
  direccion?: string;
  distrito?: string;
  provincia?: string;
  departamento?: string;

  // Cuenta
  numero_cuenta: string;
  nombre_entidad?: string;
  deuda_actual?: number;
  saldo_capital?: number;
  saldo_mora?: number;
  dias_mora?: number;
  rango_mora?: string;
  fecha_vencimiento?: string;

  // Estado
  estado?: string;
  estado_asignacion?: string;
  esta_bloqueado?: boolean;
  periodo_asignacion?: string;

  // Metadata
  fecha_ultimo_contacto?: string;
  fecha_ultimo_pago?: string;
}
```

### 8. Componente de Visualización de Cliente

**Crear**: `customer-info-panel.component.ts`

```typescript
@Component({
  selector: 'app-customer-info-panel',
  template: `
    <div class="bg-white dark:bg-gray-800 rounded-lg shadow p-4">
      <!-- Información básica -->
      <div class="grid grid-cols-2 gap-3">
        <div>
          <label class="text-xs text-gray-500">Documento</label>
          <p class="font-semibold">{{ customer().codigo_documento }}</p>
        </div>
        <div>
          <label class="text-xs text-gray-500">Nombre Completo</label>
          <p class="font-semibold">{{ customer().nombre_completo }}</p>
        </div>
      </div>

      <!-- Deuda -->
      <div class="mt-4 bg-red-50 p-3 rounded">
        <p class="text-xs text-gray-600">Deuda Total</p>
        <p class="text-2xl font-bold text-red-600">
          S/ {{ customer().deuda_actual | number:'1.2-2' }}
        </p>
        <p class="text-xs text-gray-500">
          {{ customer().dias_mora }} días de mora
        </p>
      </div>

      <!-- Teléfonos -->
      <div class="mt-4">
        <p class="text-sm font-semibold mb-2">Teléfonos</p>
        <div class="space-y-1">
          @if (customer().telefono_celular) {
            <div class="flex items-center gap-2">
              <lucide-angular name="smartphone" [size]="14"></lucide-angular>
              <span class="text-sm">{{ customer().telefono_celular }}</span>
            </div>
          }
          @if (customer().telefono_domicilio) {
            <div class="flex items-center gap-2">
              <lucide-angular name="home" [size]="14"></lucide-angular>
              <span class="text-sm">{{ customer().telefono_domicilio }}</span>
            </div>
          }
        </div>
      </div>

      <!-- Dirección -->
      @if (customer().direccion) {
        <div class="mt-4">
          <p class="text-sm font-semibold mb-1">Dirección</p>
          <p class="text-sm text-gray-600">{{ customer().direccion }}</p>
          <p class="text-xs text-gray-500">
            {{ customer().distrito }}, {{ customer().provincia }}
          </p>
        </div>
      }
    </div>
  `
})
export class CustomerInfoPanelComponent {
  customer = input.required<CustomerData>();
}
```

## 🔄 Flujo Completo

### 1. Importación de Cartera

```
1. Financiera envía archivo Excel o se conecta a tabla ASIG_TEMP
   ↓
2. Admin sube archivo o ejecuta importación desde tabla
   ↓
3. Sistema lee configuración de mapeo del tenant
   ↓
4. Sistema mapea columnas y transforma datos
   ↓
5. Sistema valida y deduplica
   ↓
6. Sistema guarda/actualiza clientes en tabla `clientes`
   ↓
7. Sistema retorna estadísticas:
   - Clientes nuevos: 150
   - Clientes actualizados: 50
   - Errores: 2
```

### 2. Visualización en Gestión

```
1. Asesor selecciona cliente de la lista
   ↓
2. Sistema carga datos completos del cliente
   ↓
3. Frontend muestra:
   - Panel de información del cliente
   - Deuda actual y días de mora
   - Teléfonos disponibles
   - Dirección
   - Historial de gestiones
   ↓
4. Asesor realiza tipificación con contexto completo
```

## 📊 Ejemplo de Uso

### Script SQL para Importar Manualmente

```sql
-- Insertar cliente de ejemplo desde ASIG_TEMP
INSERT INTO clientes (
    id_inquilino, codigo_documento, nombre_completo, numero_cuenta,
    telefono_celular, telefono_domicilio, dias_mora, deuda_actual,
    saldo_capital, saldo_mora, direccion, distrito, departamento,
    periodo_asignacion, estado, fecha_importacion
)
SELECT
    2, -- Financiera Oh
    IDENTITY_CODE,
    NOMBRE_COMPLETO,
    NUM_CUENTA_PMCP,
    TELEFONO_CELULAR,
    TELEFONO_DOMICILIO,
    CAST(DIAS_MORA_ASIG AS UNSIGNED),
    CAST(SLD_TOTAL_ASIG AS DECIMAL(15,2)),
    CAST(SLD_CAPITAL_ASIG AS DECIMAL(15,2)),
    CAST(SLD_MORA_ASIG AS DECIMAL(15,2)),
    DIRECCION,
    DISTRITO,
    DEPARTAMENTO,
    PERIODO,
    'ACTIVO',
    CURDATE()
FROM ASIG_TEMP
WHERE PERIODO = '202510'
LIMIT 1;
```

## 📝 Próximos Pasos

1. ✅ Crear `CustomerRepository`
2. ✅ Crear configuración de mapeo JSON
3. ✅ Implementar `CustomerImportService`
4. ✅ Crear endpoints REST
5. ✅ Actualizar frontend con panel de cliente
6. ✅ Integrar datos reales en gestión
7. ✅ Crear pantalla de administración de importaciones

## 🎯 Resultado Final

La asesora verá:

```
┌─────────────────────────────────────────────┐
│ Cliente: RAUL ERNESTO ARRIOLA SEVILLANO     │
│ DNI: D000041692138                          │
│ Cuenta: 4040710025347160                    │
├─────────────────────────────────────────────┤
│ 💰 Deuda Total: S/ 23,653.54                │
│ ⏰ Días Mora: 87 días (Tramo 3)             │
│ 💵 Saldo Capital: S/ 19,589.58              │
│ 🔴 Saldo Mora: S/ 5,100.00                  │
├─────────────────────────────────────────────┤
│ 📱 Celular: 949356887                       │
│ 🏠 Domicilio: 255983                        │
│ 💼 Laboral: 603490                          │
│ 📞 Ref 1: 251407                            │
├─────────────────────────────────────────────┤
│ 📍 CALLE INCA PAULO 170 URB SANTA MARIA     │
│    TRUJILLO, LA LIBERTAD                    │
├─────────────────────────────────────────────┤
│ [Historial de Gestiones]                    │
│ • 13/10/2025 - Promesa de Pago              │
│ • 10/10/2025 - Sin Contacto                 │
└─────────────────────────────────────────────┘
```

---

**Status**: Entidad creada ✅ | Pendiente: Servicios e importación
**Última actualización**: 2025-10-13
