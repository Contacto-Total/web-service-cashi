-- ========================================
-- Multi-Tenant Architecture Migration
-- ========================================

-- 1. CORE TENANT TABLES
-- ========================================

CREATE TABLE IF NOT EXISTS tenants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_code VARCHAR(50) NOT NULL UNIQUE,
    tenant_name VARCHAR(255) NOT NULL,
    business_name VARCHAR(255),
    tax_id VARCHAR(50),
    country_code VARCHAR(3),
    timezone VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    max_users INT,
    max_concurrent_sessions INT,
    subscription_tier VARCHAR(50),
    subscription_expires_at DATETIME,
    config_json JSON,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_tenant_code (tenant_code),
    INDEX idx_tenant_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS portfolios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    portfolio_code VARCHAR(50) NOT NULL,
    portfolio_name VARCHAR(255) NOT NULL,
    portfolio_type VARCHAR(50),
    parent_portfolio_id BIGINT,
    hierarchy_level INT,
    hierarchy_path VARCHAR(500),
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    config_json JSON,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_portfolio_id) REFERENCES portfolios(id) ON DELETE SET NULL,
    UNIQUE INDEX idx_portfolio_code (tenant_id, portfolio_code),
    INDEX idx_portfolio_tenant (tenant_id),
    INDEX idx_portfolio_parent (parent_portfolio_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS campaigns (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    portfolio_id BIGINT,
    campaign_code VARCHAR(50) NOT NULL,
    campaign_name VARCHAR(255) NOT NULL,
    campaign_type VARCHAR(50),
    description TEXT,
    start_date DATE,
    end_date DATE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    target_accounts INT,
    target_amount DECIMAL(15,2),
    config_json JSON,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    FOREIGN KEY (portfolio_id) REFERENCES portfolios(id) ON DELETE SET NULL,
    INDEX idx_campaign_tenant (tenant_id),
    INDEX idx_campaign_portfolio (portfolio_id),
    INDEX idx_campaign_dates (start_date, end_date),
    INDEX idx_campaign_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. DYNAMIC FIELD CONFIGURATION
-- ========================================

CREATE TABLE IF NOT EXISTS field_definitions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    field_code VARCHAR(100) NOT NULL UNIQUE,
    field_name VARCHAR(255) NOT NULL,
    field_type VARCHAR(50) NOT NULL,
    field_category VARCHAR(100),
    description TEXT,
    default_value VARCHAR(500),
    validation_rules JSON,
    display_order INT,
    is_system_field BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_field_code (field_code),
    INDEX idx_field_category (field_category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tenant_field_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    portfolio_id BIGINT,
    field_definition_id BIGINT NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    is_visible BOOLEAN NOT NULL DEFAULT TRUE,
    is_editable BOOLEAN NOT NULL DEFAULT TRUE,
    display_label VARCHAR(255),
    display_order INT,
    default_value_override VARCHAR(500),
    validation_rules_override JSON,
    config_json JSON,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    FOREIGN KEY (portfolio_id) REFERENCES portfolios(id) ON DELETE CASCADE,
    FOREIGN KEY (field_definition_id) REFERENCES field_definitions(id) ON DELETE CASCADE,
    UNIQUE INDEX idx_tenant_field_unique (tenant_id, portfolio_id, field_definition_id),
    INDEX idx_tenant_field_tenant (tenant_id),
    INDEX idx_tenant_field_portfolio (portfolio_id),
    INDEX idx_tenant_field_def (field_definition_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. CLASSIFICATION CATALOGS
-- ========================================

CREATE TABLE IF NOT EXISTS contact_classification_catalog (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    label VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    description TEXT,
    display_order INT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_contact_cat_code (code),
    INDEX idx_contact_cat_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tenant_contact_classifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    portfolio_id BIGINT,
    catalog_id BIGINT NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    custom_label VARCHAR(255),
    display_order INT,
    config_json JSON,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    FOREIGN KEY (portfolio_id) REFERENCES portfolios(id) ON DELETE CASCADE,
    FOREIGN KEY (catalog_id) REFERENCES contact_classification_catalog(id) ON DELETE CASCADE,
    UNIQUE INDEX idx_tenant_contact_unique (tenant_id, portfolio_id, catalog_id),
    INDEX idx_tenant_contact_tenant (tenant_id),
    INDEX idx_tenant_contact_portfolio (portfolio_id),
    INDEX idx_tenant_contact_catalog (catalog_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS management_classification_catalog (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    label VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    description TEXT,
    default_requires_payment BOOLEAN DEFAULT FALSE,
    default_requires_schedule BOOLEAN DEFAULT FALSE,
    default_requires_follow_up BOOLEAN DEFAULT FALSE,
    default_requires_installment_plan BOOLEAN DEFAULT FALSE,
    display_order INT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_mgmt_cat_code (code),
    INDEX idx_mgmt_cat_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tenant_management_classifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    portfolio_id BIGINT,
    catalog_id BIGINT NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    custom_label VARCHAR(255),
    requires_payment_override BOOLEAN,
    requires_schedule_override BOOLEAN,
    requires_follow_up_override BOOLEAN,
    requires_installment_plan_override BOOLEAN,
    display_order INT,
    config_json JSON,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    FOREIGN KEY (portfolio_id) REFERENCES portfolios(id) ON DELETE CASCADE,
    FOREIGN KEY (catalog_id) REFERENCES management_classification_catalog(id) ON DELETE CASCADE,
    UNIQUE INDEX idx_tenant_mgmt_unique (tenant_id, portfolio_id, catalog_id),
    INDEX idx_tenant_mgmt_tenant (tenant_id),
    INDEX idx_tenant_mgmt_portfolio (portfolio_id),
    INDEX idx_tenant_mgmt_catalog (catalog_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. UPDATE MANAGEMENTS TABLE (ADD MULTI-TENANT SUPPORT)
-- ========================================

ALTER TABLE managements
    ADD COLUMN IF NOT EXISTS tenant_id BIGINT AFTER id,
    ADD COLUMN IF NOT EXISTS portfolio_id BIGINT AFTER tenant_id,
    ADD COLUMN IF NOT EXISTS campaign_id BIGINT AFTER portfolio_id,
    ADD COLUMN IF NOT EXISTS legacy_campaign_id VARCHAR(255) AFTER advisor_id,
    ADD INDEX idx_mgmt_tenant (tenant_id),
    ADD INDEX idx_mgmt_portfolio (portfolio_id),
    ADD INDEX idx_mgmt_campaign (campaign_id),
    ADD INDEX idx_mgmt_customer (customer_id),
    ADD INDEX idx_mgmt_advisor (advisor_id),
    ADD INDEX idx_mgmt_date (management_date);

-- Add foreign keys if not exists
ALTER TABLE managements
    ADD CONSTRAINT fk_mgmt_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_mgmt_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_mgmt_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE SET NULL;

-- 5. MANAGEMENT DYNAMIC FIELDS (EAV PATTERN)
-- ========================================

CREATE TABLE IF NOT EXISTS management_dynamic_fields (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    management_id BIGINT NOT NULL,
    field_definition_id BIGINT NOT NULL,
    field_value TEXT,
    field_value_numeric DECIMAL(20,6),
    field_value_date DATETIME,
    field_value_boolean BOOLEAN,
    field_value_json JSON,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (management_id) REFERENCES managements(id) ON DELETE CASCADE,
    FOREIGN KEY (field_definition_id) REFERENCES field_definitions(id) ON DELETE CASCADE,
    UNIQUE INDEX idx_mgmt_dynamic_unique (management_id, field_definition_id),
    INDEX idx_mgmt_dynamic_management (management_id),
    INDEX idx_mgmt_dynamic_field (field_definition_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. BUSINESS RULES & STRATEGY PATTERN
-- ========================================

CREATE TABLE IF NOT EXISTS tenant_business_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    portfolio_id BIGINT,
    rule_code VARCHAR(100) NOT NULL,
    rule_name VARCHAR(255) NOT NULL,
    rule_type VARCHAR(50) NOT NULL,
    rule_category VARCHAR(100),
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    priority INT,
    condition_expression TEXT,
    action_expression TEXT,
    validation_json JSON,
    error_message VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    FOREIGN KEY (portfolio_id) REFERENCES portfolios(id) ON DELETE CASCADE,
    UNIQUE INDEX idx_rule_unique (tenant_id, portfolio_id, rule_code),
    INDEX idx_rule_tenant (tenant_id),
    INDEX idx_rule_portfolio (portfolio_id),
    INDEX idx_rule_code (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS processing_strategies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    portfolio_id BIGINT,
    strategy_type VARCHAR(100) NOT NULL,
    strategy_implementation VARCHAR(500) NOT NULL,
    strategy_name VARCHAR(255),
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    priority INT,
    config_json JSON,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    FOREIGN KEY (portfolio_id) REFERENCES portfolios(id) ON DELETE CASCADE,
    UNIQUE INDEX idx_strategy_unique (tenant_id, portfolio_id, strategy_type),
    INDEX idx_strategy_tenant (tenant_id),
    INDEX idx_strategy_portfolio (portfolio_id),
    INDEX idx_strategy_type (strategy_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. SEED DATA - DEFAULT CATALOGS
-- ========================================

-- Contact Classification Catalog (from existing data)
INSERT IGNORE INTO contact_classification_catalog (code, label, category, display_order, is_active)
VALUES
    ('CPC', 'Contacto con Cliente', 'CONTACT', 1, TRUE),
    ('CTT', 'Contacto con Tercero', 'CONTACT', 2, TRUE),
    ('BZN', 'Buzón de Voz', 'NO_CONTACT', 3, TRUE),
    ('NCC', 'No Contesta', 'NO_CONTACT', 4, TRUE),
    ('NRG', 'Número Registrado', 'WRONG_NUMBER', 5, TRUE),
    ('FDR', 'Fuera de Rango', 'WRONG_NUMBER', 6, TRUE);

-- Management Classification Catalog
INSERT IGNORE INTO management_classification_catalog
    (code, label, category, default_requires_payment, default_requires_schedule, display_order, is_active)
VALUES
    ('ACP', 'Acepta Compromiso de Pago', 'PROMISE', TRUE, TRUE, 1, TRUE),
    ('RCP', 'Rechaza Compromiso de Pago', 'PROMISE', FALSE, FALSE, 2, TRUE),
    ('PAG', 'Pago Realizado', 'PAYMENT', TRUE, FALSE, 3, TRUE),
    ('INF', 'Solicita Información', 'INFO', FALSE, FALSE, 4, TRUE),
    ('RCL', 'Reclamo', 'COMPLAINT', FALSE, TRUE, 5, TRUE),
    ('SEG', 'Seguimiento', 'FOLLOWUP', FALSE, TRUE, 6, TRUE);

-- Default Field Definitions
INSERT IGNORE INTO field_definitions (field_code, field_name, field_type, field_category, is_system_field, display_order)
VALUES
    ('PAYMENT_METHOD', 'Método de Pago', 'SELECT', 'PAYMENT', FALSE, 1),
    ('PAYMENT_REFERENCE', 'Referencia de Pago', 'TEXT', 'PAYMENT', FALSE, 2),
    ('PAYMENT_BANK', 'Banco', 'SELECT', 'PAYMENT', FALSE, 3),
    ('PAYMENT_DATE', 'Fecha de Pago', 'DATE', 'PAYMENT', FALSE, 4),
    ('CONTACT_PHONE', 'Teléfono de Contacto', 'PHONE', 'CONTACT', FALSE, 5),
    ('CONTACT_EMAIL', 'Email de Contacto', 'EMAIL', 'CONTACT', FALSE, 6),
    ('COMPLAINT_TYPE', 'Tipo de Reclamo', 'SELECT', 'COMPLAINT', FALSE, 7),
    ('COMPLAINT_REASON', 'Motivo de Reclamo', 'TEXT', 'COMPLAINT', FALSE, 8),
    ('NEXT_CONTACT_DATE', 'Próxima Fecha de Contacto', 'DATETIME', 'SCHEDULE', FALSE, 9),
    ('DEBT_AMOUNT', 'Monto de Deuda', 'CURRENCY', 'PAYMENT', FALSE, 10);
