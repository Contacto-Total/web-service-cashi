# Arquitectura Normalizada - Sistema Multi-Tenant

## Principios de Diseño

### 1. Normalización Extrema
- Eliminar redundancia de datos
- Soportar jerarquías dinámicas (N niveles de tipificación)
- Configuración granular por tenant → portfolio → sub-portfolio
- Herencia de configuración con overrides

### 2. Mantenibilidad
- CRUD completo desde UI para tipificaciones
- Versionado de configuraciones
- Auditoría completa de cambios
- Rollback capabilities

### 3. Escalabilidad
- Soporte para millones de gestiones
- Índices optimizados
- Particionamiento por tenant
- Cache strategies

## Estructura Normalizada

### NIVEL 1: Catálogos Maestros (Sistema)

```
classification_catalog (Catálogo único de todas las tipificaciones)
├── id (PK)
├── code (UNIQUE: "CTT", "ACP", etc.)
├── name ("Contacto con Tercero")
├── classification_type (ENUM: CONTACT_RESULT, MANAGEMENT_TYPE, PAYMENT_TYPE)
├── parent_classification_id (FK → classification_catalog, self-reference)
├── hierarchy_level (1, 2, 3, ... N)
├── hierarchy_path (/CTT/CTT-FAM/CTT-FAM-ESP)
├── description
├── display_order
├── icon_name
├── color_hex
├── is_system (boolean - no se puede eliminar)
├── metadata_schema (JSON - define campos requeridos para este tipo)
├── created_at
├── updated_at
└── deleted_at (soft delete)
```

**Ventajas**:
- Una sola tabla para TODAS las clasificaciones
- Jerarquía ilimitada (nivel 1, 2, 3, ... N)
- Fácil de mantener
- Queries más simples

### NIVEL 2: Configuración por Tenant/Portfolio

```
tenant_classification_config
├── id (PK)
├── tenant_id (FK → tenants)
├── portfolio_id (FK → portfolios, nullable)
├── classification_id (FK → classification_catalog)
├── is_enabled (boolean)
├── is_required (boolean)
├── custom_name (override del nombre)
├── custom_order (override del orden)
├── custom_icon
├── custom_color
├── requires_comment (boolean)
├── min_comment_length (int)
├── requires_attachment (boolean)
├── requires_followup_date (boolean)
├── auto_triggers (JSON: acciones automáticas)
├── validation_rules (JSON)
├── ui_config (JSON: configuración de visualización)
├── created_by
├── created_at
├── updated_at
└── UNIQUE(tenant_id, portfolio_id, classification_id)
```

### NIVEL 3: Reglas de Dependencia

```
classification_dependencies
├── id (PK)
├── tenant_id (FK → tenants)
├── portfolio_id (FK → portfolios, nullable)
├── parent_classification_id (FK → classification_catalog)
├── child_classification_id (FK → classification_catalog)
├── dependency_type (ENUM: REQUIRES, SUGGESTS, BLOCKS, ENABLES)
├── is_mandatory (boolean)
├── condition_expression (JSON: condiciones para aplicar)
├── display_order
├── created_at
└── updated_at

Ejemplo:
- Si selecciono "Acepta Compromiso Pago" (nivel 1)
  → REQUIRES "Tipo de Pago" (nivel 2)
  → ENABLES "Método de Pago" (nivel 3)
```

### NIVEL 4: Campos Dinámicos por Clasificación

```
classification_field_mappings
├── id (PK)
├── tenant_id (FK → tenants)
├── portfolio_id (FK → portfolios, nullable)
├── classification_id (FK → classification_catalog)
├── field_definition_id (FK → field_definitions)
├── is_required (boolean)
├── is_visible (boolean)
├── conditional_logic (JSON: cuándo mostrar)
├── display_order
├── created_at
└── updated_at

Ejemplo:
- "Acepta Compromiso Pago" requiere campos:
  - payment_amount (required)
  - payment_date (required)
  - payment_method (optional)
```

### NIVEL 5: Historial de Cambios (Auditoría)

```
classification_config_history
├── id (PK)
├── entity_type (ENUM: CLASSIFICATION, CONFIG, DEPENDENCY, FIELD_MAPPING)
├── entity_id (ID del registro afectado)
├── tenant_id
├── portfolio_id
├── change_type (ENUM: CREATE, UPDATE, DELETE, ENABLE, DISABLE)
├── changed_by (user_id)
├── previous_value (JSON)
├── new_value (JSON)
├── change_reason (TEXT)
├── ip_address
├── user_agent
├── created_at
└── INDEX(entity_type, entity_id, created_at)
```

### NIVEL 6: Versiones de Configuración

```
configuration_versions
├── id (PK)
├── tenant_id (FK → tenants)
├── portfolio_id (FK → portfolios, nullable)
├── version_number (INT)
├── version_name ("Config Q1 2025", "Post-Migration")
├── description
├── is_active (boolean)
├── snapshot_data (JSON: configuración completa)
├── created_by
├── activated_at
├── created_at
└── UNIQUE(tenant_id, portfolio_id, version_number)
```

## Relaciones Normalizadas

### Tenants → Portfolios (Jerarquía)

```
tenants
├── id
├── tenant_code (UNIQUE)
├── tenant_name
├── parent_tenant_id (FK → tenants) -- NUEVO: multi-tenant jerárquico
├── tenant_type (ENUM: ROOT, SUBSIDIARY, DEPARTMENT)
└── ...

portfolios
├── id
├── tenant_id (FK → tenants)
├── portfolio_code
├── portfolio_name
├── parent_portfolio_id (FK → portfolios)
├── hierarchy_level (1, 2, 3, ...)
├── full_path (/CREDIT_CARD/CLASSIC/GOLD)
└── ...
```

### Gestiones → Clasificaciones

```
managements
├── id
├── tenant_id
├── portfolio_id
├── campaign_id
├── customer_id
├── advisor_id
├── management_date
├── classification_level_1_id (FK → classification_catalog)
├── classification_level_2_id (FK → classification_catalog)
├── classification_level_3_id (FK → classification_catalog)
├── classification_level_4_id (FK → classification_catalog) -- hasta N
├── observations
└── ...

-- ALTERNATIVA: Más normalizada
management_classifications
├── id (PK)
├── management_id (FK → managements)
├── classification_id (FK → classification_catalog)
├── hierarchy_level (1, 2, 3, ...)
├── selected_at
└── UNIQUE(management_id, hierarchy_level)
```

## Optimizaciones

### 1. Materialized Views

```sql
CREATE MATERIALIZED VIEW mv_active_classifications_by_tenant AS
SELECT
    t.tenant_code,
    p.portfolio_code,
    c.code AS classification_code,
    c.name AS classification_name,
    c.hierarchy_level,
    c.hierarchy_path,
    tcc.is_enabled,
    tcc.is_required,
    COALESCE(tcc.custom_name, c.name) AS display_name,
    COALESCE(tcc.custom_order, c.display_order) AS display_order
FROM classification_catalog c
JOIN tenant_classification_config tcc ON c.id = tcc.classification_id
JOIN tenants t ON t.id = tcc.tenant_id
LEFT JOIN portfolios p ON p.id = tcc.portfolio_id
WHERE tcc.is_enabled = TRUE
  AND c.deleted_at IS NULL;
```

### 2. Índices Estratégicos

```sql
-- Para búsqueda de clasificaciones por tenant/portfolio
CREATE INDEX idx_tcc_tenant_portfolio_enabled
ON tenant_classification_config(tenant_id, portfolio_id, is_enabled)
WHERE is_enabled = TRUE;

-- Para jerarquías
CREATE INDEX idx_classification_hierarchy
ON classification_catalog(parent_classification_id, hierarchy_level, display_order);

-- Para path queries
CREATE INDEX idx_classification_path
ON classification_catalog USING GIN(hierarchy_path gin_trgm_ops);

-- Para auditoría
CREATE INDEX idx_history_entity_date
ON classification_config_history(entity_type, entity_id, created_at DESC);
```

### 3. Particionamiento

```sql
-- Particionar managements por tenant y fecha
CREATE TABLE managements (
    ...
) PARTITION BY RANGE (management_date);

CREATE TABLE managements_2025_q1
PARTITION OF managements
FOR VALUES FROM ('2025-01-01') TO ('2025-04-01');

-- Particionar dynamic fields por tenant
CREATE TABLE management_dynamic_fields (
    ...
) PARTITION BY HASH (tenant_id);
```

## API de Mantenimiento

### Endpoints Requeridos

```typescript
// Gestión de Catálogo
POST   /api/v1/admin/classifications
GET    /api/v1/admin/classifications
GET    /api/v1/admin/classifications/{id}
PUT    /api/v1/admin/classifications/{id}
DELETE /api/v1/admin/classifications/{id}

// Gestión de Configuración por Tenant
GET    /api/v1/admin/tenants/{tenantId}/portfolios/{portfolioId}/classifications
POST   /api/v1/admin/tenants/{tenantId}/portfolios/{portfolioId}/classifications/enable
POST   /api/v1/admin/tenants/{tenantId}/portfolios/{portfolioId}/classifications/disable
PUT    /api/v1/admin/tenants/{tenantId}/portfolios/{portfolioId}/classifications/{classificationId}

// Gestión de Dependencias
GET    /api/v1/admin/tenants/{tenantId}/portfolios/{portfolioId}/classification-dependencies
POST   /api/v1/admin/tenants/{tenantId}/portfolios/{portfolioId}/classification-dependencies
DELETE /api/v1/admin/tenants/{tenantId}/portfolios/{portfolioId}/classification-dependencies/{id}

// Gestión de Campos Dinámicos
GET    /api/v1/admin/tenants/{tenantId}/portfolios/{portfolioId}/classifications/{classificationId}/fields
POST   /api/v1/admin/tenants/{tenantId}/portfolios/{portfolioId}/classifications/{classificationId}/fields
PUT    /api/v1/admin/tenants/{tenantId}/portfolios/{portfolioId}/classifications/{classificationId}/fields/{fieldId}

// Versionado
GET    /api/v1/admin/tenants/{tenantId}/portfolios/{portfolioId}/configuration-versions
POST   /api/v1/admin/tenants/{tenantId}/portfolios/{portfolioId}/configuration-versions/snapshot
POST   /api/v1/admin/tenants/{tenantId}/portfolios/{portfolioId}/configuration-versions/{versionId}/activate
POST   /api/v1/admin/tenants/{tenantId}/portfolios/{portfolioId}/configuration-versions/{versionId}/rollback

// Auditoría
GET    /api/v1/admin/audit/classification-changes
GET    /api/v1/admin/audit/classification-changes/{entityType}/{entityId}
```

## Beneficios de esta Arquitectura

### 1. **Mantenibilidad Extrema**
- ✅ UI puede gestionar TODO sin tocar código
- ✅ Agregar nivel 4, 5, N sin cambiar schema
- ✅ Rollback a versiones anteriores
- ✅ Auditoría completa de cambios

### 2. **Flexibilidad Total**
- ✅ Tipificaciones por tenant/portfolio/sub-portfolio
- ✅ Jerarquías dinámicas (no limitadas a 3 niveles)
- ✅ Dependencias configurables
- ✅ Campos dinámicos por clasificación

### 3. **Performance**
- ✅ Materialized views para lectura rápida
- ✅ Particionamiento para escalar
- ✅ Índices optimizados
- ✅ Soporte para cache

### 4. **Compliance y Auditoría**
- ✅ Soft deletes
- ✅ Historial completo
- ✅ Versionado
- ✅ Trazabilidad de cambios

## Próximos Pasos

1. Crear entidades JPA normalizadas
2. Implementar repositorios
3. Crear servicios de administración
4. Generar migración SQL
5. Implementar API REST de mantenimiento
6. Crear DTOs y mappers
7. Implementar validaciones
