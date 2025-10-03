# Multi-Tenant Architecture Implementation

## Overview

This document describes the multi-tenant architecture implementation for the CASHI collection management system, designed to support multiple clients with varying field requirements per tenant, portfolio, and sub-portfolio.

## Architecture Decisions

### 1. Hybrid Model: Fixed Core + Dynamic Extensions

**Core Fixed Fields** (in all managements):
- Tenant ID, Portfolio ID, Campaign ID
- Customer ID, Advisor ID
- Management Date
- Contact Result (code + description)
- Management Type (code + description)
- Observations

**Dynamic Fields** (via EAV pattern):
- Tenant-specific custom fields
- Portfolio-specific fields
- Management-type-specific fields

### 2. Catalog-Based Classifications

**Master Catalogs**:
- `ContactClassificationCatalog`: System-wide contact result types
- `ManagementClassificationCatalog`: System-wide management types

**Tenant Overrides**:
- `TenantContactClassification`: Enable/disable/customize per tenant/portfolio
- `TenantManagementClassification`: Override behavior per tenant/portfolio

### 3. Metadata-Driven Configuration

**Field Definitions** (`FieldDefinition`):
- Master catalog of all possible fields
- Field types: TEXT, NUMBER, DECIMAL, DATE, BOOLEAN, SELECT, etc.
- Validation rules stored as JSON

**Tenant Field Configs** (`TenantFieldConfig`):
- Enable/disable fields per tenant/portfolio
- Override labels, default values, validation rules
- Control visibility, editability, required status

## Key Entities

### Core Multi-Tenant Entities

#### `Tenant`
```java
- tenantCode: String (unique identifier, e.g., "BANCO_UNION")
- tenantName: String
- businessName: String
- taxId: String
- countryCode: String
- timezone: String
- isActive: Boolean
- configJson: JSON (flexible tenant-level configuration)
```

#### `Portfolio`
```java
- tenant: Tenant (many-to-one)
- portfolioCode: String
- portfolioName: String
- portfolioType: Enum (CREDIT_CARD, PERSONAL_LOAN, MORTGAGE, etc.)
- parentPortfolio: Portfolio (self-reference for hierarchy)
- hierarchyLevel: Integer
- hierarchyPath: String (e.g., "/CREDIT_CARD/SUBPRIME")
- configJson: JSON
```

#### `Campaign`
```java
- tenant: Tenant
- portfolio: Portfolio (optional)
- campaignCode: String
- campaignName: String
- campaignType: Enum (EARLY_COLLECTION, LATE_COLLECTION, etc.)
- startDate: LocalDate
- endDate: LocalDate
- configJson: JSON
```

### Dynamic Field System

#### `FieldDefinition`
Master catalog of available fields:
```java
- fieldCode: String (e.g., "PAYMENT_METHOD")
- fieldName: String
- fieldType: Enum (TEXT, NUMBER, DATE, SELECT, etc.)
- fieldCategory: String (grouping)
- validationRules: JSON
- displayOrder: Integer
- isSystemField: Boolean
```

#### `TenantFieldConfig`
Per-tenant/portfolio field configuration:
```java
- tenant: Tenant
- portfolio: Portfolio (optional)
- fieldDefinition: FieldDefinition
- isEnabled: Boolean
- isRequired: Boolean
- isVisible: Boolean
- isEditable: Boolean
- displayLabel: String (override)
- validationRulesOverride: JSON
```

#### `ManagementDynamicField`
EAV storage for actual field values:
```java
- management: Management
- fieldDefinition: FieldDefinition
- fieldValue: String (for TEXT)
- fieldValueNumeric: BigDecimal (for NUMBER/DECIMAL)
- fieldValueDate: LocalDateTime (for DATE/DATETIME)
- fieldValueBoolean: Boolean (for BOOLEAN)
- fieldValueJson: JSON (for complex types)
```

### Classification System

#### `ContactClassificationCatalog`
System-wide contact results:
```java
- code: String (e.g., "CPC")
- label: String (e.g., "Contacto con Cliente")
- category: String
- displayOrder: Integer
- isActive: Boolean
```

#### `TenantContactClassification`
Enable/customize per tenant:
```java
- tenant: Tenant
- portfolio: Portfolio (optional)
- catalog: ContactClassificationCatalog
- isEnabled: Boolean
- customLabel: String (override)
- displayOrder: Integer (override)
```

#### `ManagementClassificationCatalog`
System-wide management types:
```java
- code: String (e.g., "ACP")
- label: String (e.g., "Acepta Compromiso de Pago")
- defaultRequiresPayment: Boolean
- defaultRequiresSchedule: Boolean
- defaultRequiresFollowUp: Boolean
- defaultRequiresInstallmentPlan: Boolean
```

#### `TenantManagementClassification`
Override behavior per tenant:
```java
- tenant: Tenant
- portfolio: Portfolio (optional)
- catalog: ManagementClassificationCatalog
- isEnabled: Boolean
- requiresPaymentOverride: Boolean
- requiresScheduleOverride: Boolean
- requiresFollowUpOverride: Boolean
```

### Business Rules & Strategy Pattern

#### `TenantBusinessRule`
Configurable validation and business logic:
```java
- tenant: Tenant
- portfolio: Portfolio (optional)
- ruleCode: String
- ruleName: String
- ruleType: Enum (FIELD_VALIDATION, WORKFLOW_VALIDATION, etc.)
- priority: Integer
- conditionExpression: String (evaluated at runtime)
- actionExpression: String
- validationJson: JSON
- errorMessage: String
```

#### `ProcessingStrategy`
Strategy pattern configuration:
```java
- tenant: Tenant
- portfolio: Portfolio (optional)
- strategyType: Enum (VALIDATION_STRATEGY, FIELD_GENERATION_STRATEGY, etc.)
- strategyImplementation: String (fully qualified class name)
- priority: Integer
- configJson: JSON
```

## Design Patterns

### 1. Strategy Pattern

**ValidationStrategy**:
- Interface: `ValidationStrategy`
- Factory: `ValidationStrategyFactory`
- Implementations loaded dynamically from `ProcessingStrategy` table
- Cached per tenant/portfolio for performance

**FieldGenerationStrategy**:
- Interface: `FieldGenerationStrategy`
- Factory: `FieldGenerationStrategyFactory`
- Determines which fields to show based on management type

### 2. Factory Pattern

**ValidationStrategyFactory**:
```java
public ValidationStrategy getValidationStrategy(Tenant tenant, Portfolio portfolio) {
    // 1. Check cache
    // 2. Load from ProcessingStrategy table
    // 3. Instantiate strategy class dynamically
    // 4. Cache and return
}
```

### 3. EAV (Entity-Attribute-Value) Pattern

Avoids NULL columns by storing dynamic fields separately:
- **Entity**: Management
- **Attribute**: FieldDefinition
- **Value**: ManagementDynamicField (type-specific columns)

### 4. Template Method Pattern

Base classes provide common structure:
- `AggregateRoot`: Common audit fields
- `AuditingEntityListener`: Automatic timestamp management

## JPA Optimizations

### 1. Indexed Columns

All foreign keys and frequently queried fields have indexes:
```java
@Index(name = "idx_mgmt_tenant", columnList = "tenant_id")
@Index(name = "idx_mgmt_portfolio", columnList = "portfolio_id")
@Index(name = "idx_mgmt_date", columnList = "management_date")
```

### 2. Fetch Strategies

- `FetchType.LAZY`: For all relationships (avoid N+1 queries)
- Use JOIN FETCH in queries when needed

### 3. Cascade Operations

```java
@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
private List<ManagementDynamicField> dynamicFields;
```

### 4. Query Methods

Spring Data JPA repositories with:
- Derived query methods
- Custom `@Query` with JPQL
- Proper use of `@Param`

### 5. Auditing

Automatic timestamp management:
```java
@EntityListeners(AuditingEntityListener.class)
@CreatedDate
@LastModifiedDate
```

## Usage Examples

### 1. Create a New Tenant with Portfolio

```java
// Create tenant
Tenant tenant = new Tenant("BANCO_UNION", "Banco Unión S.A.",
    "Banco Unión", "20123456789", "PE", "America/Lima");
tenantRepository.save(tenant);

// Create portfolio
Portfolio portfolio = new Portfolio(tenant, "CC_CLASSIC",
    "Tarjetas de Crédito Clásicas", Portfolio.PortfolioType.CREDIT_CARD);
portfolioRepository.save(portfolio);

// Create sub-portfolio
Portfolio subPortfolio = new Portfolio(tenant, "CC_CLASSIC_GOLD",
    "TC Clásica Gold", Portfolio.PortfolioType.CREDIT_CARD, portfolio);
portfolioRepository.save(subPortfolio);
```

### 2. Configure Dynamic Fields for Tenant

```java
// Get field definition from catalog
FieldDefinition paymentMethodField = fieldDefinitionRepository
    .findByFieldCode("PAYMENT_METHOD").orElseThrow();

// Enable for tenant/portfolio
TenantFieldConfig config = new TenantFieldConfig(
    tenant, portfolio, paymentMethodField,
    true, // enabled
    true, // required
    true  // visible
);
config.setDisplayLabel("Método de Pago Preferido");
tenantFieldConfigRepository.save(config);
```

### 3. Enable Contact Classifications

```java
// Get from catalog
ContactClassificationCatalog cpcCatalog = catalogRepository
    .findByCode("CPC").orElseThrow();

// Enable for tenant
TenantContactClassification tenantCPC = new TenantContactClassification(
    tenant, portfolio, cpcCatalog, true
);
tenantContactRepository.save(tenantCPC);
```

### 4. Create Management with Dynamic Fields

```java
// Create management with multi-tenant support
Management mgmt = new Management(
    tenant, portfolio, campaign, customerId, advisorId
);
managementRepository.save(mgmt);

// Add dynamic fields
FieldDefinition paymentMethod = fieldDefRepo.findByFieldCode("PAYMENT_METHOD").get();
ManagementDynamicField dynamicField = new ManagementDynamicField(mgmt, paymentMethod);
dynamicField.setFieldValue("YAPE");
mgmt.addDynamicField(dynamicField);
managementRepository.save(mgmt);
```

### 5. Use Validation Strategy

```java
ValidationStrategy strategy = validationStrategyFactory
    .getValidationStrategy(tenant, portfolio);

Map<String, Object> fieldValues = new HashMap<>();
fieldValues.put("PAYMENT_AMOUNT", 150.00);
fieldValues.put("PAYMENT_METHOD", "YAPE");

ValidationResult result = strategy.validate(fieldValues);
if (!result.isValid()) {
    throw new ValidationException(result.getErrors());
}
```

## Migration Strategy

### Phase 1: Catalog Setup
1. Create master field definitions
2. Create contact classification catalog
3. Create management classification catalog

### Phase 2: Tenant Onboarding
1. Create tenant record
2. Create portfolio hierarchy
3. Configure enabled fields per portfolio
4. Configure enabled classifications
5. Set up validation strategies

### Phase 3: Data Migration
1. Migrate existing managements to multi-tenant structure
2. Extract dynamic fields to EAV tables
3. Link to catalog classifications

## Performance Considerations

1. **Caching**: Strategy factories cache instantiated strategies
2. **Indexes**: All foreign keys and query fields indexed
3. **Lazy Loading**: Avoid loading unused relationships
4. **Batch Processing**: Use batch inserts for dynamic fields
5. **Query Optimization**: Use JOIN FETCH for known N+1 scenarios

## Security Considerations

1. **Tenant Isolation**: All queries must filter by tenant ID
2. **Portfolio Access**: Check user permissions per portfolio
3. **Field Visibility**: Respect `isVisible` and `isEditable` flags
4. **Audit Trail**: All changes tracked via JPA auditing

## Future Enhancements

1. **Multi-Database Tenancy**: Separate database per tenant (if needed)
2. **Dynamic Class Generation**: Generate DTOs at runtime
3. **GraphQL Support**: Flexible field selection
4. **Workflow Engine**: Advanced business rule processing
5. **Analytics**: Per-tenant reporting and dashboards
