# Sistema de Importación y Visualización de Clientes

## 📋 Descripción General

Sistema flexible de importación y visualización de datos de clientes que permite a cada tenant (financiera) definir:
- **Mapeo personalizado** de columnas desde sus archivos Excel/CSV
- **Configuración de visualización** específica para cada tenant
- **Importación automatizada** con validación y manejo de errores
- **Visualización dinámica** de datos del cliente durante la gestión

## 🏗️ Arquitectura

### Backend

#### 1. **Entidades Actualizadas**

##### Customer.java
```java
@Entity
@Table(name = "clientes")
public class Customer extends AggregateRoot {
    @Column(name = "id_inquilino", nullable = false)
    private Long tenantId;  // NUEVO: Identifica a qué financiera pertenece

    @Column(name = "codigo_documento", length = 50)
    private String documentCode;  // NUEVO: Código de documento del cliente

    @Column(name = "estado", length = 20)
    private String status;  // NUEVO: Estado del cliente (ACTIVO, INACTIVO)

    @Column(name = "fecha_importacion")
    private LocalDate importDate;  // NUEVO: Fecha de importación

    // Relaciones existentes
    @OneToOne private ContactInfo contactInfo;
    @OneToOne private AccountInfo accountInfo;
    @OneToOne private DebtInfo debtInfo;
}
```

##### ContactInfo.java
```java
@Column(name = "telefono_celular", length = 20)
private String mobilePhone;  // NUEVO: Teléfono celular principal
```

##### DebtInfo.java
```java
@Column(name = "deuda_actual", precision = 15, scale = 2)
private BigDecimal currentDebt;  // NUEVO: Deuda actual total
```

#### 2. **Servicios Nuevos**

##### CustomerImportService.java
- **Importa clientes** desde Excel (.xlsx, .xls) o CSV
- **Lee configuración del tenant** desde JSON
- **Mapea columnas** automáticamente según configuración
- **Valida datos** y maneja errores
- **Actualiza o crea** clientes según si existen

**Métodos principales:**
```java
public ImportResult importCustomers(Long tenantId, MultipartFile file, String tenantCode)
private List<Customer> importFromExcel(...)
private List<Customer> importFromCSV(...)
private Customer mapToCustomer(Long tenantId, Map<String, String> rowData, CustomerDataMapping mapping)
```

#### 3. **Controladores REST**

##### CustomerImportController.java
```
POST /api/v1/customers/import
Parameters:
  - file: MultipartFile (Excel o CSV)
  - tenantId: Long
  - tenantCode: String

Response:
{
  "success": true,
  "importedCount": 150,
  "hasErrors": false,
  "errors": [],
  "message": "150 clientes importados exitosamente"
}
```

##### CustomerController.java (Endpoints agregados)
```
GET /api/v1/customers/by-document?tenantId={id}&documentCode={code}
- Busca un cliente por código de documento dentro de un tenant

GET /api/v1/customers/display-config/{tenantCode}
- Retorna la configuración de visualización del tenant
```

### Frontend

#### 1. **Servicios**

##### CustomerDisplayService.ts
- `getCustomerById(customerId)` - Obtiene cliente por ID
- `getCustomerByDocumentCode(tenantId, documentCode)` - Busca por documento
- `getDisplayConfig(tenantCode)` - Obtiene configuración de visualización
- `importCustomers(file, tenantId, tenantCode)` - Importa archivo
- `formatValue(value, format)` - Formatea valores (currency, number, date)
- `getNestedValue(obj, path)` - Accede a propiedades anidadas

#### 2. **Componentes**

##### CustomerInfoDisplayComponent
Componente reutilizable que:
- Carga datos del cliente automáticamente
- Obtiene configuración de visualización del tenant
- Renderiza secciones dinámicamente según JSON
- Aplica formatos y estilos configurables
- Muestra destacados (highlight) en campos importantes

**Uso:**
```html
<app-customer-info-display
  [documentCode]="'D000041692138'"
  [tenantId]="2"
  [tenantCode]="'FIN-OH'">
</app-customer-info-display>
```

## 📝 Configuración JSON por Tenant

### Estructura del JSON

Cada tenant tiene un archivo JSON en `src/main/resources/tenant-configurations/{tenant-code}.json`:

```json
{
  "tenantCode": "FIN-OH",
  "tenantName": "Financiera Oh",

  "customerDataMapping": {
    "importSourceType": "EXCEL",
    "sourceTableName": "ASIG_TEMP",

    "columnMapping": {
      "CODIGO_DOCUMENTO": "documentCode",
      "NOMBRE": "fullName",
      "NRO_CUENTA": "accountNumber",
      "CELULAR": "mobilePhone",
      "DEUDA_ACTUAL": "currentDebt",
      "DIAS_MORA": "daysOverdue"
    },

    "customerDisplayConfig": {
      "title": "Información del Cliente",
      "sections": [
        {
          "sectionTitle": "Datos Personales",
          "fields": [
            {
              "field": "documentCode",
              "label": "DNI/Documento",
              "displayOrder": 1
            },
            {
              "field": "fullName",
              "label": "Nombre Completo",
              "displayOrder": 2
            }
          ]
        },
        {
          "sectionTitle": "Información de Deuda",
          "colorClass": "danger",
          "fields": [
            {
              "field": "currentDebt",
              "label": "Deuda Actual",
              "displayOrder": 1,
              "format": "currency",
              "highlight": true
            },
            {
              "field": "daysOverdue",
              "label": "Días de Mora",
              "displayOrder": 2,
              "format": "number",
              "highlight": true
            }
          ]
        }
      ]
    }
  }
}
```

### Campos de Visualización Soportados

#### Campos de Customer
- `documentCode` - Código de documento
- `fullName` - Nombre completo
- `status` - Estado del cliente

#### Campos de ContactInfo
- `contactInfo.mobilePhone` - Teléfono celular
- `contactInfo.primaryPhone` - Teléfono principal
- `contactInfo.alternativePhone` - Teléfono alternativo
- `contactInfo.workPhone` - Teléfono trabajo
- `contactInfo.email` - Correo electrónico
- `contactInfo.address` - Dirección

#### Campos de AccountInfo
- `accountInfo.accountNumber` - Número de cuenta
- `accountInfo.productType` - Tipo de producto
- `accountInfo.disbursementDate` - Fecha de desembolso
- `accountInfo.originalAmount` - Monto original
- `accountInfo.termMonths` - Plazo en meses
- `accountInfo.interestRate` - Tasa de interés

#### Campos de DebtInfo
- `debtInfo.currentDebt` - Deuda actual
- `debtInfo.capitalBalance` - Saldo capital
- `debtInfo.overdueInterest` - Interés vencido
- `debtInfo.accumulatedLateFees` - Moras acumuladas
- `debtInfo.collectionFees` - Gastos de cobranza
- `debtInfo.totalBalance` - Saldo total
- `debtInfo.daysOverdue` - Días de mora
- `debtInfo.lastPaymentDate` - Fecha último pago
- `debtInfo.lastPaymentAmount` - Monto último pago

### Formatos Soportados

- `currency` - Formato moneda (S/ 23,653.54)
- `number` - Formato número (87)
- `date` - Formato fecha (DD/MM/YYYY)
- `text` - Texto sin formato

### Clases de Color para Secciones

- `danger` - Rojo (para deudas, alertas)
- `warning` - Naranja (para advertencias)
- `success` - Verde (para información positiva)
- Sin colorClass - Azul por defecto

## 🚀 Flujo de Uso

### 1. Importación de Clientes

```bash
POST /api/v1/customers/import
Content-Type: multipart/form-data

file: clientes_financiera_oh.xlsx
tenantId: 2
tenantCode: FIN-OH
```

**Proceso:**
1. Backend recibe archivo
2. Lee configuración de `tenant-configurations/fin-oh.json`
3. Obtiene mapeo de columnas del `columnMapping`
4. Lee cada fila del Excel/CSV
5. Mapea columnas origen → campos destino
6. Valida datos requeridos
7. Verifica si cliente existe (por tenantId + documentCode)
8. Actualiza o crea cliente
9. Guarda en base de datos
10. Retorna resultado con errores si los hay

### 2. Visualización durante Gestión

```typescript
// En collection-management.page.ts
<app-customer-info-display
  [documentCode]="customerData().numero_documento"
  [tenantId]="selectedTenantId"
  [tenantCode]="'FIN-OH'">
</app-customer-info-display>
```

**Proceso:**
1. Componente recibe documentCode y tenantId
2. Llama a `GET /api/v1/customers/by-document?tenantId=2&documentCode=D000041692138`
3. Llama a `GET /api/v1/customers/display-config/FIN-OH`
4. Renderiza secciones según configuración JSON
5. Formatea valores según `format` especificado
6. Aplica estilos según `colorClass` y `highlight`

## 📦 Dependencias Agregadas

```xml
<!-- Apache POI para Excel -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>

<!-- Apache Commons CSV -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-csv</artifactId>
    <version>1.10.0</version>
</dependency>
```

## ✅ Ventajas del Sistema

### 1. **Flexibilidad Total por Tenant**
Cada financiera puede tener:
- Nombres de columnas diferentes en sus archivos
- Campos adicionales específicos
- Visualización personalizada
- Énfasis en campos importantes para su negocio

### 2. **Fácil Mantenimiento**
- Solo editar JSON, no código
- Copiar y modificar para nuevos tenants
- Sin recompilación ni despliegue

### 3. **Validación Automática**
- Campos requeridos
- Detección de duplicados
- Actualización de clientes existentes
- Reporte detallado de errores

### 4. **Reutilizable**
- Componente de visualización independiente
- Servicio de importación genérico
- Configuración declarativa

### 5. **Escalable**
- Maneja miles de registros
- Importación por lotes
- Caché de configuración

## 📋 Ejemplo Completo

### Archivo Excel de Financiera Oh
```
CODIGO_DOCUMENTO | NOMBRE                         | NRO_CUENTA       | CELULAR   | DEUDA_ACTUAL | DIAS_MORA
D000041692138    | RAUL ERNESTO ARRIOLA SEVILLANO | 4040710025347160 | 949356887 | 23653.54     | 87
```

### Configuración JSON (columnMapping)
```json
{
  "CODIGO_DOCUMENTO": "documentCode",
  "NOMBRE": "fullName",
  "NRO_CUENTA": "accountNumber",
  "CELULAR": "mobilePhone",
  "DEUDA_ACTUAL": "currentDebt",
  "DIAS_MORA": "daysOverdue"
}
```

### Resultado en Base de Datos
```sql
-- Tabla: clientes
id_inquilino: 2
codigo_documento: 'D000041692138'
nombre_completo: 'RAUL ERNESTO ARRIOLA SEVILLANO'
estado: 'ACTIVO'

-- Tabla: informacion_contacto
telefono_celular: '949356887'

-- Tabla: informacion_cuenta
numero_cuenta: '4040710025347160'

-- Tabla: informacion_deuda
deuda_actual: 23653.54
dias_mora: 87
```

### Visualización en Frontend
```
┌─────────────────────────────────────────┐
│ 👤 Información del Cliente      [ACTIVO]│
├─────────────────────────────────────────┤
│ Datos Personales                        │
│ DNI/Documento: D000041692138            │
│ Nombre: RAUL ERNESTO ARRIOLA SEVILLANO  │
├─────────────────────────────────────────┤
│ Información de Contacto                 │
│ ⚡ Celular: 949356887                   │
├─────────────────────────────────────────┤
│ ⚠️ Información de Deuda                 │
│ ⚡ Deuda Actual: S/ 23,653.54           │
│ ⚡ Días de Mora: 87                     │
│ Nro. Cuenta: 4040710025347160           │
└─────────────────────────────────────────┘
```

## 🔄 Próximos Pasos

1. ✅ Sistema de importación implementado
2. ✅ Visualización dinámica implementada
3. ✅ Configuración JSON por tenant
4. ⏳ Probar importación con datos reales
5. ⏳ Agregar más tenants de ejemplo
6. ⏳ Implementar importación desde base de datos (ASIG_TEMP)
7. ⏳ Agregar validaciones de negocio adicionales
8. ⏳ Implementar sincronización automática mensual

## 📝 Notas Importantes

- **Todos los nombres de columnas en BD están en español**
- **El sistema es multi-tenant**: Cada cliente pertenece a un tenant específico
- **Actualización automática**: Si el cliente ya existe, se actualiza su información
- **Manejo de errores**: Los errores no detienen la importación, se reportan al final
- **Configuración centralizada**: Todo está en JSON, nada hardcodeado
