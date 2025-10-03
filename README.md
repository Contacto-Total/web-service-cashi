# Cashi - Sistema de Gestión de Cobranzas

## 📋 Descripción

**Cashi** es un sistema integral de gestión de cobranzas desarrollado con arquitectura **Domain-Driven Design (DDD)** que permite a las empresas gestionar eficientemente sus procesos de recuperación de cartera.

## 🏗️ Arquitectura

El sistema implementa **DDD** con los siguientes bounded contexts:

### 1. **Shared** (Capa Compartida)
- Base classes: `AggregateRoot`, `ValueObject`
- Configuración JPA, CORS, manejo global de excepciones

### 2. **SystemConfiguration** (Configuración del Sistema)
- **Aggregates**: Campaign
- **Entities**: ContactClassification, ManagementClassification
- **Funcionalidad**: Configuración de campañas, tipificaciones de contacto y gestión

### 3. **CustomerManagement** (Gestión de Clientes)
- **Aggregates**: Customer
- **Entities**: ContactInfo, AccountInfo, DebtInfo
- **Value Objects**: DocumentNumber
- **Funcionalidad**: Gestión completa de información de clientes y deudas

### 4. **CollectionManagement** (Gestión de Cobranzas) ⭐ Principal
- **Aggregates**: Management
- **Entities**: CallDetail, PaymentDetail
- **Value Objects**: ManagementId, ContactResult, ManagementType, PaymentMethod
- **Funcionalidad**: Registro de gestiones de cobranza, llamadas y detalles de pago

### 5. **PaymentProcessing** (Procesamiento de Pagos)
- **Aggregates**: Payment, PaymentSchedule
- **Entities**: Installment
- **Value Objects**: PaymentId, PaymentScheduleId, PaymentStatus, TransactionId
- **Funcionalidad**: Gestión de pagos y cronogramas de pago

## 🛠️ Stack Tecnológico

### Backend
- **Java 21**
- **Spring Boot 4.0.0-SNAPSHOT**
- **Spring Data JPA / Hibernate**
- **MySQL 8**
- **Lombok**
- **SpringDoc OpenAPI (Swagger)**
- **Maven**

### Frontend
- **Angular 20.3**
- **Tailwind CSS 4**
- **TypeScript 5.7**
- **lucide-angular** (iconos)

## 📁 Estructura del Proyecto

```
web-service-cashi/
├── src/main/java/com/cashi/
│   ├── shared/
│   │   ├── domain/
│   │   │   ├── AggregateRoot.java
│   │   │   └── ValueObject.java
│   │   └── infrastructure/
│   │       ├── JpaConfiguration.java
│   │       ├── CorsConfiguration.java
│   │       └── GlobalExceptionHandler.java
│   │
│   ├── systemconfiguration/
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   ├── aggregates/ (Campaign)
│   │   │   │   ├── entities/ (ContactClassification, ManagementClassification)
│   │   │   │   └── enums/ (ContactClassificationEnum, ManagementClassificationEnum)
│   │   │   └── services/
│   │   ├── application/internal/queryservices/
│   │   ├── infrastructure/persistence/
│   │   │   ├── jpa/repositories/
│   │   │   └── DataSeeder.java
│   │   └── interfaces/rest/
│   │       ├── resources/
│   │       ├── transform/
│   │       └── controllers/
│   │
│   ├── customermanagement/ (misma estructura DDD)
│   ├── collectionmanagement/ (misma estructura DDD)
│   └── paymentprocessing/ (misma estructura DDD)
│
└── src/main/resources/
    └── application.properties
```

## 🚀 Instalación y Configuración

### Prerrequisitos

- JDK 21
- MySQL 8
- Node.js 20+ y npm
- Maven

### Backend (Spring Boot)

1. **Clonar el repositorio**
```bash
cd web-service-cashi
```

2. **Configurar base de datos**

Crear base de datos en MySQL:
```sql
CREATE DATABASE cashi_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

3. **Configurar `application.properties`**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/cashi_db
spring.datasource.username=root
spring.datasource.password=tu_password
spring.jpa.hibernate.ddl-auto=update
```

4. **Ejecutar aplicación**
```bash
mvn spring-boot:run
```

El backend estará disponible en `http://localhost:8080`

**📖 Documentación Swagger UI:** `http://localhost:8080/swagger-ui.html`

### Frontend (Angular)

1. **Navegar a la carpeta del frontend**
```bash
cd ../cashi
```

2. **Instalar dependencias**
```bash
npm install
```

3. **Ejecutar aplicación**
```bash
npm start
```

El frontend estará disponible en `http://localhost:4200`

## 📡 API Endpoints

### System Configuration
```
GET  /api/v1/system-config/contact-classifications
GET  /api/v1/system-config/management-classifications
GET  /api/v1/system-config/campaigns
GET  /api/v1/system-config/campaigns/{campaignId}
```

### Customer Management
```
GET  /api/v1/customers
GET  /api/v1/customers/{customerId}
GET  /api/v1/customers/document/{documentNumber}
GET  /api/v1/customers/search?query=
```

### Collection Management
```
POST /api/v1/managements
GET  /api/v1/managements/{managementId}
PUT  /api/v1/managements/{managementId}
GET  /api/v1/managements/customer/{customerId}
GET  /api/v1/managements/advisor/{advisorId}
GET  /api/v1/managements/campaign/{campaignId}
POST /api/v1/managements/{managementId}/call/start
POST /api/v1/managements/{managementId}/call/end
POST /api/v1/managements/{managementId}/payment
```

### Payment Processing
```
POST /api/v1/payments
GET  /api/v1/payments/{paymentId}
GET  /api/v1/payments/customer/{customerId}
GET  /api/v1/payments/customer/{customerId}/pending
POST /api/v1/payments/{paymentId}/confirm
POST /api/v1/payments/{paymentId}/cancel

POST /api/v1/payments/schedules
GET  /api/v1/payments/schedules/{scheduleId}
GET  /api/v1/payments/schedules/customer/{customerId}
POST /api/v1/payments/schedules/{scheduleId}/installments
POST /api/v1/payments/schedules/{scheduleId}/cancel
```

## 📖 Documentación de API (Swagger)

Una vez ejecutado el backend, acceder a la documentación interactiva:

**Swagger UI:** `http://localhost:8080/swagger-ui.html`

### Características de la Documentación:

✅ **Interfaz interactiva** - Prueba todos los endpoints directamente desde el navegador
✅ **Schemas completos** - Visualiza todas las estructuras de datos (DTOs)
✅ **Ejemplos de requests** - Cada endpoint incluye ejemplos de uso
✅ **Códigos de respuesta** - Documentación completa de responses (200, 201, 404, etc.)
✅ **Agrupación por bounded context** - Los endpoints están organizados por:
- **System Configuration**: Campañas y tipificaciones
- **Customer Management**: Gestión de clientes
- **Collection Management**: Gestión de cobranzas
- **Payment Processing**: Procesamiento de pagos

## 🎯 Funcionalidades Principales

### ✅ Gestión de Clientes
- Registro y búsqueda de clientes
- Información de contacto, cuenta y deuda
- Historial de pagos

### ✅ Gestión de Cobranzas
- Registro de gestiones de cobranza
- Control de llamadas (inicio/fin, duración)
- Tipificación de contacto y gestión
- Registro de compromisos de pago
- Observaciones y notas

### ✅ Procesamiento de Pagos
- Registro de pagos individuales
- Creación de cronogramas de pago
- Seguimiento de cuotas
- Confirmación y cancelación de pagos

### ✅ Configuración del Sistema
- Gestión de campañas
- Tipificaciones de contacto
- Tipificaciones de gestión
- Inicialización automática de datos mediante `DataSeeder`

## 🔐 Seguridad

- CORS configurado para desarrollo
- Manejo global de excepciones
- Validación de datos en DTOs

## 🧪 Testing

```bash
# Backend
mvn test

# Frontend
npm test
```

## 📝 Patrones de Diseño Implementados

- **Domain-Driven Design (DDD)**
- **CQRS** (Command Query Responsibility Segregation)
- **Repository Pattern**
- **DTO Pattern**
- **Assembler Pattern** (para transformación entity ↔ DTO)
- **Value Object Pattern**
- **Aggregate Root Pattern**

## 🤝 Contribuciones

Para contribuir al proyecto:

1. Fork el repositorio
2. Crear una rama feature (`git checkout -b feature/NuevaFuncionalidad`)
3. Commit los cambios (`git commit -m 'Add: Nueva funcionalidad'`)
4. Push a la rama (`git push origin feature/NuevaFuncionalidad`)
5. Abrir un Pull Request

## 📄 Licencia

Este proyecto es privado y confidencial.

## 👥 Equipo

Desarrollado por el equipo de Contacto Total

---

**Versión:** 1.0.0
**Última actualización:** Enero 2025
