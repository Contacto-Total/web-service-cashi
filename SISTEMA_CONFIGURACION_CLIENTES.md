# Sistema de Configuración Dinámica por Cliente

## 📋 Resumen

Sistema modular que permite crear y configurar tipificaciones e inputs dinámicos para nuevos clientes de forma rápida mediante archivos JSON.

## 🎯 Objetivo

Facilitar la creación de configuraciones para nuevos clientes sin necesidad de modificar código Java. Todo se hace mediante archivos JSON que el sistema lee automáticamente.

## 📁 Estructura Creada

```
web-service-cashi/src/main/resources/tenant-configurations/
├── TEMPLATE.json           ← Plantilla base para copiar
├── financiera-oh.json      ← Configuración completa de Financiera Oh
└── [nuevo-cliente].json    ← Agregar archivos aquí para nuevos clientes
```

## 📄 Formato del JSON

Cada archivo JSON contiene:

### 1. Información del Tenant
```json
{
  "tenantCode": "FIN-OH",
  "tenantName": "Financiera Oh",
  "businessName": "Financiera Oh S.A.",
  "description": "Descripción...",
  "classifications": [...]
}
```

### 2. Clasificaciones (Tipificaciones)
```json
{
  "code": "PT",
  "label": "Pago Total",
  "hierarchyLevel": 3,
  "parentCode": "PC",
  "colorHex": "#10B981",
  "iconName": "check-circle",
  "displayOrder": 1,
  "businessRules": {...},
  "dynamicFields": [...]
}
```

### 3. Reglas de Negocio (businessRules)

Las reglas definen el comportamiento de cada tipificación:

| Regla | Descripción | Ejemplo de Uso |
|-------|-------------|----------------|
| `requiresPayment` | Requiere captura de pago | `PT`, `PP`, `PPT` |
| `requiresSchedule` | Requiere cronograma | `PF`, `CF` |
| `requiresFollowUp` | Crea tarea de seguimiento | `PU`, `PF`, `CD_SMT` |
| `requiresApproval` | Requiere autorización | `CF`, `EA_*` |
| `requiresDocumentation` | Requiere documentos | `EA_ENF`, `EA_DES` |
| `allowDuplicates` | Permite múltiples registros | `true`/`false` |
| `preventDuplicateSchedule` | **Evita cronogramas duplicados** | `PF`, `CF` |
| `closesAccount` | Cierra la cuenta | `PT` |
| `validatePaymentAmount` | Valida monto vs deuda | `PT`, `PP` |
| `createsFollowUpTask` | Crea tarea automática | `PU`, `PF`, `CD_SMT` |
| `escalationRequired` | Requiere escalamiento | `CN`, `PI`, `ESC` |
| `affectsCustomerScore` | Afecta calificación | `PI` |
| `updatesCustomerPhone` | Actualiza teléfono | `AD_NT` |
| `updatesCustomerAddress` | Actualiza dirección | `AD_ND` |
| `requiresLegalReview` | Revisión legal | `EA_LEG` |
| `requiresRetry` | Reintento automático | `SC`, `SR` |

### 4. Campos Dinámicos (dynamicFields)

Define los inputs que se capturan para cada tipificación:

```json
{
  "fieldCode": "monto_pagado",
  "fieldName": "Monto Pagado",
  "fieldType": "currency",
  "isRequired": true,
  "description": "Monto total pagado",
  "displayOrder": 1,
  "defaultValue": null,
  "options": [],
  "validationRules": {
    "min": 0,
    "mustMatchDebtAmount": true
  }
}
```

#### Tipos de Campos Disponibles

**Simples:**
- `text` - Texto simple
- `textarea` - Texto multilínea
- `number` - Número entero
- `decimal` - Número decimal
- `currency` - Moneda (con formato)
- `date` - Fecha
- `datetime` - Fecha y hora
- `email` - Correo electrónico
- `phone` - Teléfono
- `url` - URL
- `checkbox` - Casilla de verificación

**Selección:**
- `select` - Lista desplegable
- `multiselect` - Selección múltiple

**Especiales:**
- `json` - Objeto JSON
- `table` - Tabla con filas/columnas
- `auto-number` - Autonumérico

#### Reglas de Validación

```json
"validationRules": {
  "min": 0,                           // Valor mínimo (number, currency)
  "max": 100,                         // Valor máximo
  "minLength": 4,                     // Longitud mínima (text)
  "maxLength": 50,                    // Longitud máxima
  "pattern": "^[0-9]{8}$",           // Expresión regular
  "minDate": "today",                 // Fecha mínima
  "maxDate": "2025-12-31",           // Fecha máxima
  "mustMatchDebtAmount": true,        // Debe coincidir con deuda
  "unique": true                      // Valor único
}
```

## 🚀 Cómo Crear un Nuevo Cliente

### Paso 1: Copiar la Plantilla

```bash
cd src/main/resources/tenant-configurations/
cp TEMPLATE.json banco-xyz.json
```

### Paso 2: Configurar Información Básica

Editar `banco-xyz.json`:

```json
{
  "tenantCode": "BANCO-XYZ",
  "tenantName": "Banco XYZ",
  "businessName": "Banco XYZ S.A.",
  "description": "Configuración de Banco XYZ",
  "classifications": [...]
}
```

### Paso 3: Definir Tipificaciones

#### Ejemplo: Resultado Positivo → Pago Confirmado → Pago Total

```json
{
  "classifications": [
    {
      "code": "RP",
      "label": "Resultado Positivo",
      "hierarchyLevel": 1,
      "parentCode": null,
      "colorHex": "#10B981",
      "iconName": "check-circle",
      "displayOrder": 1,
      "businessRules": {
        "isSuccessful": true,
        "mainCategory": "RESULTADO_POSITIVO"
      }
    },
    {
      "code": "PC",
      "label": "Pago Confirmado",
      "hierarchyLevel": 2,
      "parentCode": "RP",
      "colorHex": "#059669",
      "iconName": "dollar-sign",
      "displayOrder": 1,
      "businessRules": {
        "requiresPayment": true
      }
    },
    {
      "code": "PT",
      "label": "Pago Total",
      "hierarchyLevel": 3,
      "parentCode": "PC",
      "colorHex": "#10B981",
      "iconName": "check-circle",
      "displayOrder": 1,
      "businessRules": {
        "requiresPayment": true,
        "closesAccount": true,
        "allowDuplicates": false
      },
      "dynamicFields": [
        {
          "fieldCode": "monto_pagado",
          "fieldName": "Monto Pagado",
          "fieldType": "currency",
          "isRequired": true,
          "displayOrder": 1
        },
        {
          "fieldCode": "metodo_pago",
          "fieldName": "Método de Pago",
          "fieldType": "select",
          "isRequired": true,
          "displayOrder": 2,
          "options": ["Efectivo", "Transferencia", "Tarjeta"]
        }
      ]
    }
  ]
}
```

### Paso 4: Reiniciar el Backend

```bash
mvn spring-boot:run
```

El DataSeeder leerá automáticamente el archivo y creará:
- ✅ El tenant
- ✅ Todas las clasificaciones con su jerarquía
- ✅ Los campos dinámicos
- ✅ Las reglas de negocio

## 📊 Casos de Uso Comunes

### 1. Pago con Cronograma (Evitar Duplicados)

```json
{
  "code": "PF",
  "label": "Pago Fraccionado",
  "businessRules": {
    "requiresSchedule": true,
    "preventDuplicateSchedule": true,  // ← No permite crear otro cronograma si existe uno activo
    "allowDuplicates": false
  },
  "dynamicFields": [...]
}
```

### 2. Promesa de Pago con Seguimiento Automático

```json
{
  "code": "PU",
  "label": "Pago Único",
  "businessRules": {
    "requiresFollowUp": true,
    "createsFollowUpTask": true  // ← Crea tarea automática para la fecha comprometida
  },
  "dynamicFields": [
    {
      "fieldCode": "fecha_compromiso",
      "fieldName": "Fecha de Compromiso",
      "fieldType": "date",
      "isRequired": true,
      "validationRules": {
        "minDate": "today"  // ← No permite fechas pasadas
      }
    }
  ]
}
```

### 3. Excepción que Requiere Aprobación

```json
{
  "code": "EA_ENF",
  "label": "Enfermedad",
  "businessRules": {
    "requiresApproval": true,       // ← Requiere autorización de supervisor
    "requiresDocumentation": true,  // ← Debe adjuntar documentos
    "requiresFollowUp": true
  }
}
```

### 4. Actualización de Datos

```json
{
  "code": "AD_NT",
  "label": "Nuevo Teléfono",
  "businessRules": {
    "requiresDataUpdate": true,
    "updatesCustomerPhone": true  // ← Actualiza automáticamente el teléfono del cliente
  },
  "dynamicFields": [
    {
      "fieldCode": "nuevo_telefono",
      "fieldName": "Nuevo Teléfono",
      "fieldType": "phone",
      "isRequired": true
    },
    {
      "fieldCode": "tipo_telefono",
      "fieldName": "Tipo",
      "fieldType": "select",
      "options": ["Celular Personal", "Celular Trabajo", "Casa"]
    }
  ]
}
```

## 🔧 Implementación del DataSeeder (Pendiente)

### Código a Agregar

El `DataSeeder.java` necesita:

1. **Inyectar ObjectMapper en el constructor:**

```java
private final ObjectMapper objectMapper;

public DataSeeder(..., ObjectMapper objectMapper) {
    // ...
    this.objectMapper = objectMapper;
}
```

2. **Método para leer archivos JSON:**

```java
private void seedTenantsFromJson() {
    logger.info("====================================================================");
    logger.info("SEEDING TENANTS FROM JSON FILES");
    logger.info("====================================================================");

    try {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:tenant-configurations/*.json");

        for (Resource resource : resources) {
            String filename = resource.getFilename();

            // Ignorar TEMPLATE.json
            if (filename == null || filename.equals("TEMPLATE.json")) {
                continue;
            }

            logger.info("📄 Processing: {}", filename);

            try {
                JsonNode rootNode = objectMapper.readTree(resource.getInputStream());

                // Leer información del tenant
                String tenantCode = rootNode.get("tenantCode").asText();
                String tenantName = rootNode.get("tenantName").asText();
                String businessName = rootNode.get("businessName").asText();

                // Crear o actualizar tenant
                Tenant tenant = tenantRepository.findByCode(tenantCode)
                    .orElseGet(() -> {
                        Tenant newTenant = new Tenant(tenantCode, tenantName);
                        newTenant.setBusinessName(businessName);
                        newTenant.setIsActive(true);
                        return tenantRepository.save(newTenant);
                    });

                logger.info("  ✓ Tenant: {} ({})", tenantCode, tenantName);

                // Leer y crear clasificaciones
                JsonNode classifications = rootNode.get("classifications");
                if (classifications != null && classifications.isArray()) {
                    seedClassificationsFromJson(tenant, classifications);
                }

            } catch (Exception e) {
                logger.error("❌ Error processing {}: {}", filename, e.getMessage());
            }
        }

    } catch (IOException e) {
        logger.error("❌ Error reading JSON files: {}", e.getMessage());
    }
}

private void seedClassificationsFromJson(Tenant tenant, JsonNode classificationsNode) {
    // Procesar en orden de hierarchyLevel para asegurar que padres se creen primero
    // ...implementar lógica similar a seedFinancieraOhClassifications()
}
```

3. **Llamar al método en run():**

```java
@Override
public void run(String... args) {
    seedFieldTypes();
    seedTenants();
    seedTenantsFromJson();  // ← NUEVO
    // ...resto del código
}
```

## 📚 Ejemplos de JSON en `financiera-oh.json`

Ver archivo completo: `tenant-configurations/financiera-oh.json`

Contiene 30+ tipificaciones completas con:
- ✅ Jerarquía de 3 niveles
- ✅ Campos dinámicos configurados
- ✅ Reglas de negocio definidas
- ✅ Validaciones específicas

## 🎯 Ventajas del Sistema

1. **Rapidez**: Crear nuevo cliente en 15 minutos (copiar, modificar JSON, reiniciar)
2. **Sin Código**: No necesitas modificar Java para agregar tipificaciones
3. **Reutilizable**: Copiar configuraciones entre clientes similares
4. **Versionable**: Los JSON están en Git, puedes ver historial de cambios
5. **Validable**: JSON es fácil de validar y revisar
6. **Documentado**: El JSON mismo es la documentación

## 📝 TODOs

- [ ] Completar implementación de `seedTenantsFromJson()` en DataSeeder
- [ ] Agregar validación de JSON (schema validation)
- [ ] Crear endpoint REST para subir nuevos JSON sin reiniciar
- [ ] Agregar UI en frontend para editar JSON visualmente
- [ ] Crear más ejemplos de clientes (banco, cooperativa, retail)

## 🔗 Archivos Relacionados

- `tenant-configurations/financiera-oh.json` - Configuración completa
- `tenant-configurations/TEMPLATE.json` - Plantilla para copiar
- `DataSeeder.java` - Seeder que lee los JSON
- `ClassificationCatalog.java` - Entidad de clasificaciones
- `FinancieraOhClassificationEnum.java` - Enum legacy (deprecar)

---

**Última Actualización**: 2025-10-13
**Autor**: Sistema Cashi
**Versión**: 1.0
