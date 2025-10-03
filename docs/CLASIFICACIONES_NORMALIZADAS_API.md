# API de Clasificaciones Normalizadas - Documentación Completa

## Arquitectura Implementada

### ✅ Entidades JPA (7 entidades)
1. **ClassificationCatalog** - Catálogo unificado de tipificaciones (N niveles)
2. **TenantClassificationConfig** - Configuración por tenant/portfolio
3. **ClassificationDependency** - Dependencias entre clasificaciones
4. **ClassificationFieldMapping** - Campos dinámicos por clasificación
5. **ClassificationConfigHistory** - Auditoría de cambios
6. **ConfigurationVersion** - Versionado y snapshots
7. **ManagementClassification** - Vincula gestiones con clasificaciones

### ✅ Repositorios Spring Data JPA (7 repositorios)
- Todos con métodos derivados y queries JPQL optimizadas
- Soporte para jerarquías ilimitadas
- Filtrado por tenant/portfolio/nivel

### ✅ Servicios de Aplicación (2 servicios)
1. **ClassificationCommandServiceImpl** - Comandos (CREATE, UPDATE, DELETE, ENABLE, DISABLE)
2. **ClassificationQueryServiceImpl** - Consultas (GET por nivel, tipo, jerarquía)

### ✅ DTOs y Resources (5 DTOs)
- ClassificationCatalogResource
- TenantClassificationConfigResource
- CreateClassificationCommand
- UpdateClassificationConfigCommand
- CreateSnapshotCommand

### ✅ Controller REST Completo
- 20+ endpoints listos para usar
- Versionado, auditoría, snapshots

---

## Endpoints Disponibles

### 📋 Gestión del Catálogo (System-wide)

#### Obtener todas las clasificaciones
```http
GET /api/v1/admin/classifications
```

#### Obtener clasificaciones por tipo
```http
GET /api/v1/admin/classifications/type/{type}
```
Tipos disponibles: `CONTACT_RESULT`, `MANAGEMENT_TYPE`, `PAYMENT_TYPE`, `COMPLAINT_TYPE`, `CUSTOM`

#### Obtener clasificaciones raíz (nivel 1)
```http
GET /api/v1/admin/classifications/type/{type}/root
```

#### Obtener hijos de una clasificación
```http
GET /api/v1/admin/classifications/{parentId}/children
```

#### Crear nueva clasificación
```http
POST /api/v1/admin/classifications
Headers: X-User-Id: admin@example.com

{
  "code": "CTT-FAM",
  "name": "Tercero - Familiar",
  "classificationType": "CONTACT_RESULT",
  "parentClassificationId": 2,
  "description": "Contacto con un familiar del cliente",
  "displayOrder": 1,
  "iconName": "users",
  "colorHex": "#3B82F6"
}
```

#### Eliminar clasificación (soft delete)
```http
DELETE /api/v1/admin/classifications/{id}
Headers: X-User-Id: admin@example.com
```

---

### 🏢 Configuración por Tenant/Portfolio

#### Obtener clasificaciones habilitadas de un tenant/portfolio
```http
GET /api/v1/admin/tenants/{tenantId}/classifications?portfolioId={portfolioId}
```

**Ejemplo:**
```http
GET /api/v1/admin/tenants/1/classifications?portfolioId=5
```

#### Obtener clasificaciones por tipo para tenant/portfolio
```http
GET /api/v1/admin/tenants/{tenantId}/classifications/type/{type}?portfolioId={portfolioId}
```

**Ejemplo:**
```http
GET /api/v1/admin/tenants/1/classifications/type/CONTACT_RESULT?portfolioId=5
```

#### Obtener clasificaciones por nivel jerárquico
```http
GET /api/v1/admin/tenants/{tenantId}/classifications/level/{level}?portfolioId={portfolioId}
```

**Ejemplos:**
```http
GET /api/v1/admin/tenants/1/classifications/level/1?portfolioId=5  # Nivel 1
GET /api/v1/admin/tenants/1/classifications/level/2?portfolioId=5  # Nivel 2
GET /api/v1/admin/tenants/1/classifications/level/3?portfolioId=5  # Nivel 3
```

#### Obtener hijos habilitados de una clasificación padre
```http
GET /api/v1/admin/tenants/{tenantId}/classifications/{parentId}/children?portfolioId={portfolioId}
```

#### Habilitar clasificación para tenant/portfolio
```http
POST /api/v1/admin/tenants/{tenantId}/classifications/{classificationId}/enable?portfolioId={portfolioId}
Headers: X-User-Id: admin@example.com
```

#### Deshabilitar clasificación para tenant/portfolio
```http
POST /api/v1/admin/tenants/{tenantId}/classifications/{classificationId}/disable?portfolioId={portfolioId}
Headers: X-User-Id: admin@example.com
```

#### Actualizar configuración de clasificación
```http
PUT /api/v1/admin/tenants/{tenantId}/classifications/{classificationId}/config?portfolioId={portfolioId}
Headers: X-User-Id: admin@example.com

{
  "customName": "Contacto con Familiar Directo",
  "customOrder": 10,
  "customIcon": "family",
  "customColor": "#10B981",
  "requiresComment": true,
  "minCommentLength": 50,
  "requiresAttachment": false,
  "requiresFollowupDate": true
}
```

---

### 📸 Versionado y Snapshots

#### Obtener historial de versiones
```http
GET /api/v1/admin/tenants/{tenantId}/configuration-versions?portfolioId={portfolioId}
```

#### Obtener versión activa
```http
GET /api/v1/admin/tenants/{tenantId}/configuration-versions/active?portfolioId={portfolioId}
```

#### Crear snapshot de configuración
```http
POST /api/v1/admin/tenants/{tenantId}/configuration-versions/snapshot?portfolioId={portfolioId}
Headers: X-User-Id: admin@example.com

{
  "versionName": "Config Q1 2025",
  "description": "Configuración para campaña del primer trimestre 2025"
}
```

#### Activar versión (rollback)
```http
POST /api/v1/admin/tenants/{tenantId}/configuration-versions/{versionId}/activate
Headers: X-User-Id: admin@example.com
```

---

### 📝 Auditoría e Historial

#### Obtener historial de cambios (paginado)
```http
GET /api/v1/admin/tenants/{tenantId}/audit/changes?portfolioId={portfolioId}&page=0&size=20
```

#### Obtener historial de una entidad específica
```http
GET /api/v1/admin/audit/entity/{entityType}/{entityId}
```

**Tipos de entidad:** `CLASSIFICATION`, `CONFIG`, `DEPENDENCY`, `FIELD_MAPPING`, `VERSION`

**Ejemplo:**
```http
GET /api/v1/admin/audit/entity/CONFIG/123
```

---

## Casos de Uso Completos

### Caso 1: Configurar Tipificaciones para Nueva Sub-cartera

**Escenario:** Banco Unión crea una nueva sub-cartera "TC Platinum" y quiere copiar configuración de "TC Gold"

```bash
# 1. Obtener tenant y portfolio IDs
TENANT_ID=1
PORTFOLIO_GOLD_ID=5
PORTFOLIO_PLATINUM_ID=6

# 2. Crear snapshot de configuración actual de Gold
curl -X POST "http://localhost:8080/api/v1/admin/tenants/$TENANT_ID/configuration-versions/snapshot?portfolioId=$PORTFOLIO_GOLD_ID" \
  -H "X-User-Id: admin@bancounion.com" \
  -H "Content-Type: application/json" \
  -d '{
    "versionName": "Base TC Platinum",
    "description": "Configuración inicial copiada de TC Gold"
  }'

# 3. Obtener clasificaciones habilitadas en Gold
curl "http://localhost:8080/api/v1/admin/tenants/$TENANT_ID/classifications?portfolioId=$PORTFOLIO_GOLD_ID"

# 4. Habilitar clasificaciones específicas para Platinum
curl -X POST "http://localhost:8080/api/v1/admin/tenants/$TENANT_ID/classifications/1/enable?portfolioId=$PORTFOLIO_PLATINUM_ID" \
  -H "X-User-Id: admin@bancounion.com"

# 5. Personalizar nombres para Platinum
curl -X PUT "http://localhost:8080/api/v1/admin/tenants/$TENANT_ID/classifications/1/config?portfolioId=$PORTFOLIO_PLATINUM_ID" \
  -H "X-User-Id: admin@bancounion.com" \
  -H "Content-Type: application/json" \
  -d '{
    "customName": "Cliente Premium Contactado",
    "customIcon": "crown",
    "customColor": "#A855F7",
    "requiresComment": true,
    "minCommentLength": 100
  }'
```

### Caso 2: Gestión de Jerarquía de 3 Niveles

**Escenario:** Agregar tipificación de 3er nivel "CTT-FAM-ESP" (Tercero - Esposa)

```bash
# 1. Obtener padre (CTT-FAM)
curl "http://localhost:8080/api/v1/admin/classifications/code/CTT-FAM"
# Response: { "id": 10, "code": "CTT-FAM", ... }

# 2. Crear hijo de nivel 3
curl -X POST "http://localhost:8080/api/v1/admin/classifications" \
  -H "X-User-Id: admin@example.com" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "CTT-FAM-ESP",
    "name": "Tercero - Esposa",
    "classificationType": "CONTACT_RESULT",
    "parentClassificationId": 10,
    "displayOrder": 1
  }'

# 3. Habilitar para sub-cartera específica
curl -X POST "http://localhost:8080/api/v1/admin/tenants/1/classifications/15/enable?portfolioId=5" \
  -H "X-User-Id: admin@bancounion.com"

# 4. Obtener hijos disponibles en runtime para UI
curl "http://localhost:8080/api/v1/admin/tenants/1/classifications/10/children?portfolioId=5"
```

### Caso 3: Rollback de Configuración

**Escenario:** Se hicieron cambios incorrectos, necesitamos volver a versión anterior

```bash
# 1. Ver historial de versiones
curl "http://localhost:8080/api/v1/admin/tenants/1/configuration-versions?portfolioId=5"

# 2. Ver cambios recientes
curl "http://localhost:8080/api/v1/admin/tenants/1/audit/changes?portfolioId=5&page=0&size=10"

# 3. Activar versión anterior
curl -X POST "http://localhost:8080/api/v1/admin/tenants/1/configuration-versions/3/activate" \
  -H "X-User-Id: admin@bancounion.com"
```

---

## Integración con Frontend (Angular)

### Service TypeScript

```typescript
// classification-config.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ClassificationConfigService {
  private apiUrl = '/api/v1/admin';

  constructor(private http: HttpClient) {}

  getEnabledClassifications(tenantId: number, portfolioId: number, level: number): Observable<any[]> {
    return this.http.get<any[]>(
      `${this.apiUrl}/tenants/${tenantId}/classifications/level/${level}?portfolioId=${portfolioId}`
    );
  }

  getChildrenClassifications(tenantId: number, portfolioId: number, parentId: number): Observable<any[]> {
    return this.http.get<any[]>(
      `${this.apiUrl}/tenants/${tenantId}/classifications/${parentId}/children?portfolioId=${portfolioId}`
    );
  }

  enableClassification(tenantId: number, portfolioId: number, classificationId: number): Observable<any> {
    return this.http.post(
      `${this.apiUrl}/tenants/${tenantId}/classifications/${classificationId}/enable?portfolioId=${portfolioId}`,
      null,
      { headers: { 'X-User-Id': 'current-user@example.com' } }
    );
  }

  updateConfig(tenantId: number, portfolioId: number, classificationId: number, config: any): Observable<any> {
    return this.http.put(
      `${this.apiUrl}/tenants/${tenantId}/classifications/${classificationId}/config?portfolioId=${portfolioId}`,
      config,
      { headers: { 'X-User-Id': 'current-user@example.com' } }
    );
  }
}
```

---

## Resumen

✅ **Backend 100% funcional** para módulo de mantenimiento
✅ **20+ endpoints REST** documentados
✅ **Jerarquías ilimitadas** (nivel 1, 2, 3... N)
✅ **Configuración granular** por tenant → portfolio → sub-portfolio
✅ **Auditoría completa** con historial de cambios
✅ **Versionado y rollback** de configuraciones
✅ **Validaciones** de negocio implementadas
✅ **Soft deletes** para recuperación de datos

**Todo listo para crear el frontend de mantenimiento.**
