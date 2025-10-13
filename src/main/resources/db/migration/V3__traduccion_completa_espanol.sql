-- ========================================
-- MIGRACIÓN V3: Traducción Completa al Español
-- Traduce nombres de tablas, columnas y valores ENUM al español
-- ========================================

-- ========================================
-- PARTE 0: RENOMBRAR TABLAS
-- ========================================

-- Tablas principales
RENAME TABLE tenants TO inquilinos;
RENAME TABLE portfolios TO carteras;
RENAME TABLE campaigns TO campañas;

-- Tablas de clasificación
RENAME TABLE classification_catalog TO catalogo_clasificaciones;
RENAME TABLE tenant_classification_config TO configuracion_clasificacion_inquilino;
RENAME TABLE classification_config_history TO historial_configuracion_clasificacion;
RENAME TABLE classification_dependencies TO dependencias_clasificacion;
RENAME TABLE classification_field_mappings TO mapeos_campo_clasificacion;
RENAME TABLE configuration_versions TO versiones_configuracion;

-- Tablas de catálogos de clasificación (consolidadas)
RENAME TABLE contact_classification_catalog TO catalogo_clasificacion_contacto;
RENAME TABLE tenant_contact_classifications TO clasificaciones_contacto_inquilino;
RENAME TABLE management_classification_catalog TO catalogo_clasificacion_gestion;
RENAME TABLE tenant_management_classifications TO clasificaciones_gestion_inquilino;

-- Tablas de campos dinámicos
RENAME TABLE field_definitions TO definiciones_campos;
RENAME TABLE tenant_field_configs TO configuracion_campos_inquilino;

-- Tablas de gestión
RENAME TABLE managements TO gestiones;
RENAME TABLE management_dynamic_fields TO campos_dinamicos_gestion;
RENAME TABLE management_classifications TO clasificaciones_gestion;
RENAME TABLE call_details TO detalles_llamada;
RENAME TABLE payment_details TO detalles_pago;

-- Tablas de reglas y estrategias
RENAME TABLE tenant_business_rules TO reglas_negocio_inquilino;
RENAME TABLE processing_strategies TO estrategias_procesamiento;

-- Tablas de clientes
RENAME TABLE customers TO clientes;
RENAME TABLE customer_contact_info TO informacion_contacto;
RENAME TABLE customer_account_info TO informacion_cuenta;
RENAME TABLE customer_debt_info TO informacion_deuda;

-- Tablas de pagos
RENAME TABLE payments TO pagos;
RENAME TABLE payment_schedules TO cronogramas_pago;
RENAME TABLE installments TO cuotas;

-- ========================================
-- PARTE 1: RENOMBRAR COLUMNAS DE TABLAS PRINCIPALES
-- ========================================

-- Tabla: inquilinos (antes tenants)
ALTER TABLE inquilinos CHANGE COLUMN tenant_code codigo_inquilino VARCHAR(50) NOT NULL;
ALTER TABLE inquilinos CHANGE COLUMN tenant_name nombre_inquilino VARCHAR(255) NOT NULL;
ALTER TABLE inquilinos CHANGE COLUMN business_name razon_social VARCHAR(255);
ALTER TABLE inquilinos CHANGE COLUMN tax_id numero_fiscal VARCHAR(50);
ALTER TABLE inquilinos CHANGE COLUMN country_code codigo_pais VARCHAR(3);
ALTER TABLE inquilinos CHANGE COLUMN timezone zona_horaria VARCHAR(50);
ALTER TABLE inquilinos CHANGE COLUMN is_active esta_activo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE inquilinos CHANGE COLUMN max_users maximo_usuarios INT;
ALTER TABLE inquilinos CHANGE COLUMN max_concurrent_sessions maximo_sesiones_concurrentes INT;
ALTER TABLE inquilinos CHANGE COLUMN subscription_tier nivel_suscripcion VARCHAR(50);
ALTER TABLE inquilinos CHANGE COLUMN subscription_expires_at fecha_expiracion_suscripcion DATETIME;
ALTER TABLE inquilinos CHANGE COLUMN config_json configuracion_json JSON;
ALTER TABLE inquilinos CHANGE COLUMN created_at fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE inquilinos CHANGE COLUMN updated_at fecha_actualizacion DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- Recrear índices con nuevos nombres
DROP INDEX idx_tenant_code ON inquilinos;
DROP INDEX idx_tenant_active ON inquilinos;
CREATE INDEX idx_codigo_inquilino ON inquilinos(codigo_inquilino);
CREATE INDEX idx_inquilino_activo ON inquilinos(esta_activo);

-- Tabla: carteras (antes portfolios)
ALTER TABLE carteras CHANGE COLUMN tenant_id id_inquilino BIGINT NOT NULL;
ALTER TABLE carteras CHANGE COLUMN portfolio_code codigo_cartera VARCHAR(50) NOT NULL;
ALTER TABLE carteras CHANGE COLUMN portfolio_name nombre_cartera VARCHAR(255) NOT NULL;
ALTER TABLE carteras CHANGE COLUMN portfolio_type tipo_cartera VARCHAR(50);
ALTER TABLE carteras CHANGE COLUMN parent_portfolio_id id_cartera_padre BIGINT;
ALTER TABLE carteras CHANGE COLUMN hierarchy_level nivel_jerarquia INT;
ALTER TABLE carteras CHANGE COLUMN hierarchy_path ruta_jerarquia VARCHAR(500);
ALTER TABLE carteras CHANGE COLUMN description descripcion TEXT;
ALTER TABLE carteras CHANGE COLUMN is_active esta_activo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE carteras CHANGE COLUMN config_json configuracion_json JSON;
ALTER TABLE carteras CHANGE COLUMN created_at fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE carteras CHANGE COLUMN updated_at fecha_actualizacion DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- Recrear foreign keys e índices
ALTER TABLE carteras DROP FOREIGN KEY portfolios_ibfk_1;
ALTER TABLE carteras DROP FOREIGN KEY portfolios_ibfk_2;
DROP INDEX idx_portfolio_code ON carteras;
DROP INDEX idx_portfolio_tenant ON carteras;
DROP INDEX idx_portfolio_parent ON carteras;

ALTER TABLE carteras ADD CONSTRAINT fk_cartera_inquilino FOREIGN KEY (id_inquilino) REFERENCES inquilinos(id) ON DELETE CASCADE;
ALTER TABLE carteras ADD CONSTRAINT fk_cartera_padre FOREIGN KEY (id_cartera_padre) REFERENCES carteras(id) ON DELETE SET NULL;
CREATE UNIQUE INDEX idx_codigo_cartera ON carteras(id_inquilino, codigo_cartera);
CREATE INDEX idx_cartera_inquilino ON carteras(id_inquilino);
CREATE INDEX idx_cartera_padre ON carteras(id_cartera_padre);

-- Tabla: campaigns -> campañas
ALTER TABLE campaigns CHANGE COLUMN tenant_id id_inquilino BIGINT NOT NULL;
ALTER TABLE campaigns CHANGE COLUMN portfolio_id id_cartera BIGINT;
ALTER TABLE campaigns CHANGE COLUMN campaign_code codigo_campana VARCHAR(50) NOT NULL;
ALTER TABLE campaigns CHANGE COLUMN campaign_name nombre_campana VARCHAR(255) NOT NULL;
ALTER TABLE campaigns CHANGE COLUMN campaign_type tipo_campana VARCHAR(50);
ALTER TABLE campaigns CHANGE COLUMN description descripcion TEXT;
ALTER TABLE campaigns CHANGE COLUMN start_date fecha_inicio DATE;
ALTER TABLE campaigns CHANGE COLUMN end_date fecha_fin DATE;
ALTER TABLE campaigns CHANGE COLUMN is_active esta_activo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE campaigns CHANGE COLUMN target_accounts cuentas_objetivo INT;
ALTER TABLE campaigns CHANGE COLUMN target_amount monto_objetivo DECIMAL(15,2);
ALTER TABLE campaigns CHANGE COLUMN config_json configuracion_json JSON;
ALTER TABLE campaigns CHANGE COLUMN created_at fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE campaigns CHANGE COLUMN updated_at fecha_actualizacion DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- Recrear foreign keys e índices
ALTER TABLE campaigns DROP FOREIGN KEY campaigns_ibfk_1;
ALTER TABLE campaigns DROP FOREIGN KEY campaigns_ibfk_2;
DROP INDEX idx_campaign_tenant ON campaigns;
DROP INDEX idx_campaign_portfolio ON campaigns;
DROP INDEX idx_campaign_dates ON campaigns;
DROP INDEX idx_campaign_active ON campaigns;

ALTER TABLE campaigns ADD CONSTRAINT fk_campana_inquilino FOREIGN KEY (id_inquilino) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE campaigns ADD CONSTRAINT fk_campana_cartera FOREIGN KEY (id_cartera) REFERENCES portfolios(id) ON DELETE SET NULL;
CREATE INDEX idx_campana_inquilino ON campaigns(id_inquilino);
CREATE INDEX idx_campana_cartera ON campaigns(id_cartera);
CREATE INDEX idx_campana_fechas ON campaigns(fecha_inicio, fecha_fin);
CREATE INDEX idx_campana_activo ON campaigns(esta_activo);

-- ========================================
-- PARTE 2: CLASSIFICATION_CATALOG (Catálogo Unificado)
-- ========================================

ALTER TABLE classification_catalog CHANGE COLUMN code codigo VARCHAR(20) NOT NULL;
ALTER TABLE classification_catalog CHANGE COLUMN name nombre VARCHAR(255) NOT NULL;
ALTER TABLE classification_catalog CHANGE COLUMN classification_type tipo_clasificacion VARCHAR(50) NOT NULL;
ALTER TABLE classification_catalog CHANGE COLUMN parent_classification_id id_clasificacion_padre BIGINT;
ALTER TABLE classification_catalog CHANGE COLUMN hierarchy_level nivel_jerarquia INT NOT NULL;
ALTER TABLE classification_catalog CHANGE COLUMN hierarchy_path ruta_jerarquia VARCHAR(1000);
ALTER TABLE classification_catalog CHANGE COLUMN description descripcion TEXT;
ALTER TABLE classification_catalog CHANGE COLUMN display_order orden_visualizacion INT;
ALTER TABLE classification_catalog CHANGE COLUMN icon_name nombre_icono VARCHAR(100);
ALTER TABLE classification_catalog CHANGE COLUMN color_hex color_hexadecimal VARCHAR(7);
ALTER TABLE classification_catalog CHANGE COLUMN is_system es_sistema BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE classification_catalog CHANGE COLUMN metadata_schema esquema_metadatos JSON;
ALTER TABLE classification_catalog CHANGE COLUMN is_active esta_activo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE classification_catalog CHANGE COLUMN created_at fecha_creacion DATETIME NOT NULL;
ALTER TABLE classification_catalog CHANGE COLUMN updated_at fecha_actualizacion DATETIME;
ALTER TABLE classification_catalog CHANGE COLUMN deleted_at fecha_eliminacion DATETIME;

-- Recrear índices
DROP INDEX idx_classification_code ON classification_catalog;
DROP INDEX idx_classification_type ON classification_catalog;
DROP INDEX idx_classification_parent ON classification_catalog;
DROP INDEX idx_classification_hierarchy ON classification_catalog;

CREATE UNIQUE INDEX idx_codigo_clasificacion ON classification_catalog(codigo);
CREATE INDEX idx_tipo_clasificacion ON classification_catalog(tipo_clasificacion);
CREATE INDEX idx_clasificacion_padre ON classification_catalog(id_clasificacion_padre);
CREATE INDEX idx_jerarquia_clasificacion ON classification_catalog(nivel_jerarquia, orden_visualizacion);

-- Recrear foreign key
ALTER TABLE classification_catalog DROP FOREIGN KEY classification_catalog_ibfk_1;
ALTER TABLE classification_catalog ADD CONSTRAINT fk_clasificacion_padre
    FOREIGN KEY (id_clasificacion_padre) REFERENCES classification_catalog(id) ON DELETE CASCADE;

-- ========================================
-- PARTE 3: TENANT_CLASSIFICATION_CONFIG
-- ========================================

ALTER TABLE tenant_classification_config CHANGE COLUMN tenant_id id_inquilino BIGINT NOT NULL;
ALTER TABLE tenant_classification_config CHANGE COLUMN portfolio_id id_cartera BIGINT;
ALTER TABLE tenant_classification_config CHANGE COLUMN classification_id id_clasificacion BIGINT NOT NULL;
ALTER TABLE tenant_classification_config CHANGE COLUMN is_enabled esta_habilitado BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE tenant_classification_config CHANGE COLUMN custom_name nombre_personalizado VARCHAR(255);
ALTER TABLE tenant_classification_config CHANGE COLUMN custom_icon icono_personalizado VARCHAR(100);
ALTER TABLE tenant_classification_config CHANGE COLUMN custom_color color_personalizado VARCHAR(7);
ALTER TABLE tenant_classification_config CHANGE COLUMN display_order orden_visualizacion INT;
ALTER TABLE tenant_classification_config CHANGE COLUMN requires_comment requiere_comentario BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tenant_classification_config CHANGE COLUMN min_comment_length longitud_minima_comentario INT;
ALTER TABLE tenant_classification_config CHANGE COLUMN max_comment_length longitud_maxima_comentario INT;
ALTER TABLE tenant_classification_config CHANGE COLUMN validation_rules reglas_validacion JSON;
ALTER TABLE tenant_classification_config CHANGE COLUMN created_at fecha_creacion DATETIME NOT NULL;
ALTER TABLE tenant_classification_config CHANGE COLUMN updated_at fecha_actualizacion DATETIME;

-- Recrear foreign keys e índices
ALTER TABLE tenant_classification_config DROP FOREIGN KEY tenant_classification_config_ibfk_1;
ALTER TABLE tenant_classification_config DROP FOREIGN KEY tenant_classification_config_ibfk_2;
ALTER TABLE tenant_classification_config DROP FOREIGN KEY tenant_classification_config_ibfk_3;

DROP INDEX idx_tenant_class_config ON tenant_classification_config;
DROP INDEX idx_tenant_class_tenant ON tenant_classification_config;
DROP INDEX idx_tenant_class_portfolio ON tenant_classification_config;
DROP INDEX idx_tenant_class_classification ON tenant_classification_config;

ALTER TABLE tenant_classification_config ADD CONSTRAINT fk_config_inquilino
    FOREIGN KEY (id_inquilino) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE tenant_classification_config ADD CONSTRAINT fk_config_cartera
    FOREIGN KEY (id_cartera) REFERENCES portfolios(id) ON DELETE CASCADE;
ALTER TABLE tenant_classification_config ADD CONSTRAINT fk_config_clasificacion
    FOREIGN KEY (id_clasificacion) REFERENCES classification_catalog(id) ON DELETE CASCADE;

CREATE UNIQUE INDEX idx_config_clasificacion_unico ON tenant_classification_config(id_inquilino, id_cartera, id_clasificacion);
CREATE INDEX idx_config_inquilino ON tenant_classification_config(id_inquilino);
CREATE INDEX idx_config_cartera ON tenant_classification_config(id_cartera);
CREATE INDEX idx_config_clasificacion ON tenant_classification_config(id_clasificacion);

-- ========================================
-- PARTE 4: CLASSIFICATION_CONFIG_HISTORY
-- ========================================

ALTER TABLE classification_config_history CHANGE COLUMN tenant_id id_inquilino BIGINT NOT NULL;
ALTER TABLE classification_config_history CHANGE COLUMN portfolio_id id_cartera BIGINT;
ALTER TABLE classification_config_history CHANGE COLUMN entity_type tipo_entidad VARCHAR(50) NOT NULL;
ALTER TABLE classification_config_history CHANGE COLUMN entity_id id_entidad BIGINT NOT NULL;
ALTER TABLE classification_config_history CHANGE COLUMN change_type tipo_cambio VARCHAR(50) NOT NULL;
ALTER TABLE classification_config_history CHANGE COLUMN previous_state estado_anterior JSON;
ALTER TABLE classification_config_history CHANGE COLUMN new_state estado_nuevo JSON;
ALTER TABLE classification_config_history CHANGE COLUMN changed_by cambiado_por VARCHAR(255) NOT NULL;
ALTER TABLE classification_config_history CHANGE COLUMN changed_at fecha_cambio DATETIME NOT NULL;
ALTER TABLE classification_config_history CHANGE COLUMN change_reason razon_cambio TEXT;

-- Recrear índices
DROP INDEX idx_history_tenant ON classification_config_history;
DROP INDEX idx_history_entity ON classification_config_history;
DROP INDEX idx_history_date ON classification_config_history;

CREATE INDEX idx_historial_inquilino ON classification_config_history(id_inquilino);
CREATE INDEX idx_historial_entidad ON classification_config_history(tipo_entidad, id_entidad);
CREATE INDEX idx_historial_fecha ON classification_config_history(fecha_cambio);

-- ========================================
-- PARTE 5: FIELD_DEFINITIONS
-- ========================================

ALTER TABLE field_definitions CHANGE COLUMN field_code codigo_campo VARCHAR(100) NOT NULL;
ALTER TABLE field_definitions CHANGE COLUMN field_name nombre_campo VARCHAR(255) NOT NULL;
ALTER TABLE field_definitions CHANGE COLUMN field_type tipo_campo VARCHAR(50) NOT NULL;
ALTER TABLE field_definitions CHANGE COLUMN field_category categoria_campo VARCHAR(100);
ALTER TABLE field_definitions CHANGE COLUMN description descripcion TEXT;
ALTER TABLE field_definitions CHANGE COLUMN default_value valor_por_defecto VARCHAR(500);
ALTER TABLE field_definitions CHANGE COLUMN validation_rules reglas_validacion JSON;
ALTER TABLE field_definitions CHANGE COLUMN display_order orden_visualizacion INT;
ALTER TABLE field_definitions CHANGE COLUMN is_system_field es_campo_sistema BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE field_definitions CHANGE COLUMN created_at fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE field_definitions CHANGE COLUMN updated_at fecha_actualizacion DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- Recrear índices
DROP INDEX idx_field_code ON field_definitions;
DROP INDEX idx_field_category ON field_definitions;
CREATE UNIQUE INDEX idx_codigo_campo ON field_definitions(codigo_campo);
CREATE INDEX idx_categoria_campo ON field_definitions(categoria_campo);

-- ========================================
-- PARTE 6: TENANT_FIELD_CONFIGS
-- ========================================

ALTER TABLE tenant_field_configs CHANGE COLUMN tenant_id id_inquilino BIGINT NOT NULL;
ALTER TABLE tenant_field_configs CHANGE COLUMN portfolio_id id_cartera BIGINT;
ALTER TABLE tenant_field_configs CHANGE COLUMN field_definition_id id_definicion_campo BIGINT NOT NULL;
ALTER TABLE tenant_field_configs CHANGE COLUMN is_enabled esta_habilitado BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE tenant_field_configs CHANGE COLUMN is_required es_requerido BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tenant_field_configs CHANGE COLUMN is_visible es_visible BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE tenant_field_configs CHANGE COLUMN is_editable es_editable BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE tenant_field_configs CHANGE COLUMN display_label etiqueta_visualizacion VARCHAR(255);
ALTER TABLE tenant_field_configs CHANGE COLUMN display_order orden_visualizacion INT;
ALTER TABLE tenant_field_configs CHANGE COLUMN default_value_override valor_defecto_personalizado VARCHAR(500);
ALTER TABLE tenant_field_configs CHANGE COLUMN validation_rules_override reglas_validacion_personalizado JSON;
ALTER TABLE tenant_field_configs CHANGE COLUMN config_json configuracion_json JSON;
ALTER TABLE tenant_field_configs CHANGE COLUMN created_at fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE tenant_field_configs CHANGE COLUMN updated_at fecha_actualizacion DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- Recrear foreign keys e índices
ALTER TABLE tenant_field_configs DROP FOREIGN KEY tenant_field_configs_ibfk_1;
ALTER TABLE tenant_field_configs DROP FOREIGN KEY tenant_field_configs_ibfk_2;
ALTER TABLE tenant_field_configs DROP FOREIGN KEY tenant_field_configs_ibfk_3;

DROP INDEX idx_tenant_field_unique ON tenant_field_configs;
DROP INDEX idx_tenant_field_tenant ON tenant_field_configs;
DROP INDEX idx_tenant_field_portfolio ON tenant_field_configs;
DROP INDEX idx_tenant_field_def ON tenant_field_configs;

ALTER TABLE tenant_field_configs ADD CONSTRAINT fk_campo_config_inquilino
    FOREIGN KEY (id_inquilino) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE tenant_field_configs ADD CONSTRAINT fk_campo_config_cartera
    FOREIGN KEY (id_cartera) REFERENCES portfolios(id) ON DELETE CASCADE;
ALTER TABLE tenant_field_configs ADD CONSTRAINT fk_campo_config_definicion
    FOREIGN KEY (id_definicion_campo) REFERENCES field_definitions(id) ON DELETE CASCADE;

CREATE UNIQUE INDEX idx_campo_config_unico ON tenant_field_configs(id_inquilino, id_cartera, id_definicion_campo);
CREATE INDEX idx_campo_config_inquilino ON tenant_field_configs(id_inquilino);
CREATE INDEX idx_campo_config_cartera ON tenant_field_configs(id_cartera);
CREATE INDEX idx_campo_config_definicion ON tenant_field_configs(id_definicion_campo);

-- ========================================
-- PARTE 7: CONTACT & MANAGEMENT CLASSIFICATION CATALOGS (Legacy)
-- ========================================

ALTER TABLE contact_classification_catalog CHANGE COLUMN code codigo VARCHAR(10) NOT NULL;
ALTER TABLE contact_classification_catalog CHANGE COLUMN label etiqueta VARCHAR(255) NOT NULL;
ALTER TABLE contact_classification_catalog CHANGE COLUMN category categoria VARCHAR(100);
ALTER TABLE contact_classification_catalog CHANGE COLUMN description descripcion TEXT;
ALTER TABLE contact_classification_catalog CHANGE COLUMN display_order orden_visualizacion INT;
ALTER TABLE contact_classification_catalog CHANGE COLUMN is_active esta_activo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE contact_classification_catalog CHANGE COLUMN created_at fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE contact_classification_catalog CHANGE COLUMN updated_at fecha_actualizacion DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

DROP INDEX idx_contact_cat_code ON contact_classification_catalog;
DROP INDEX idx_contact_cat_category ON contact_classification_catalog;
CREATE UNIQUE INDEX idx_codigo_contacto ON contact_classification_catalog(codigo);
CREATE INDEX idx_categoria_contacto ON contact_classification_catalog(categoria);

ALTER TABLE management_classification_catalog CHANGE COLUMN code codigo VARCHAR(10) NOT NULL;
ALTER TABLE management_classification_catalog CHANGE COLUMN label etiqueta VARCHAR(255) NOT NULL;
ALTER TABLE management_classification_catalog CHANGE COLUMN category categoria VARCHAR(100);
ALTER TABLE management_classification_catalog CHANGE COLUMN description descripcion TEXT;
ALTER TABLE management_classification_catalog CHANGE COLUMN default_requires_payment requiere_pago_por_defecto BOOLEAN DEFAULT FALSE;
ALTER TABLE management_classification_catalog CHANGE COLUMN default_requires_schedule requiere_cronograma_por_defecto BOOLEAN DEFAULT FALSE;
ALTER TABLE management_classification_catalog CHANGE COLUMN default_requires_follow_up requiere_seguimiento_por_defecto BOOLEAN DEFAULT FALSE;
ALTER TABLE management_classification_catalog CHANGE COLUMN default_requires_installment_plan requiere_plan_cuotas_por_defecto BOOLEAN DEFAULT FALSE;
ALTER TABLE management_classification_catalog CHANGE COLUMN display_order orden_visualizacion INT;
ALTER TABLE management_classification_catalog CHANGE COLUMN is_active esta_activo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE management_classification_catalog CHANGE COLUMN created_at fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE management_classification_catalog CHANGE COLUMN updated_at fecha_actualizacion DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

DROP INDEX idx_mgmt_cat_code ON management_classification_catalog;
DROP INDEX idx_mgmt_cat_category ON management_classification_catalog;
CREATE UNIQUE INDEX idx_codigo_gestion ON management_classification_catalog(codigo);
CREATE INDEX idx_categoria_gestion ON management_classification_catalog(categoria);

-- ========================================
-- PARTE 8: TENANT CONTACT/MANAGEMENT CLASSIFICATIONS
-- ========================================

ALTER TABLE tenant_contact_classifications CHANGE COLUMN tenant_id id_inquilino BIGINT NOT NULL;
ALTER TABLE tenant_contact_classifications CHANGE COLUMN portfolio_id id_cartera BIGINT;
ALTER TABLE tenant_contact_classifications CHANGE COLUMN catalog_id id_catalogo BIGINT NOT NULL;
ALTER TABLE tenant_contact_classifications CHANGE COLUMN is_enabled esta_habilitado BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE tenant_contact_classifications CHANGE COLUMN custom_label etiqueta_personalizada VARCHAR(255);
ALTER TABLE tenant_contact_classifications CHANGE COLUMN display_order orden_visualizacion INT;
ALTER TABLE tenant_contact_classifications CHANGE COLUMN config_json configuracion_json JSON;
ALTER TABLE tenant_contact_classifications CHANGE COLUMN created_at fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE tenant_contact_classifications CHANGE COLUMN updated_at fecha_actualizacion DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE tenant_contact_classifications DROP FOREIGN KEY tenant_contact_classifications_ibfk_1;
ALTER TABLE tenant_contact_classifications DROP FOREIGN KEY tenant_contact_classifications_ibfk_2;
ALTER TABLE tenant_contact_classifications DROP FOREIGN KEY tenant_contact_classifications_ibfk_3;

DROP INDEX idx_tenant_contact_unique ON tenant_contact_classifications;
DROP INDEX idx_tenant_contact_tenant ON tenant_contact_classifications;
DROP INDEX idx_tenant_contact_portfolio ON tenant_contact_classifications;
DROP INDEX idx_tenant_contact_catalog ON tenant_contact_classifications;

ALTER TABLE tenant_contact_classifications ADD CONSTRAINT fk_contacto_inquilino
    FOREIGN KEY (id_inquilino) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE tenant_contact_classifications ADD CONSTRAINT fk_contacto_cartera
    FOREIGN KEY (id_cartera) REFERENCES portfolios(id) ON DELETE CASCADE;
ALTER TABLE tenant_contact_classifications ADD CONSTRAINT fk_contacto_catalogo
    FOREIGN KEY (id_catalogo) REFERENCES contact_classification_catalog(id) ON DELETE CASCADE;

CREATE UNIQUE INDEX idx_contacto_unico ON tenant_contact_classifications(id_inquilino, id_cartera, id_catalogo);
CREATE INDEX idx_contacto_inquilino ON tenant_contact_classifications(id_inquilino);
CREATE INDEX idx_contacto_cartera ON tenant_contact_classifications(id_cartera);
CREATE INDEX idx_contacto_catalogo ON tenant_contact_classifications(id_catalogo);

ALTER TABLE tenant_management_classifications CHANGE COLUMN tenant_id id_inquilino BIGINT NOT NULL;
ALTER TABLE tenant_management_classifications CHANGE COLUMN portfolio_id id_cartera BIGINT;
ALTER TABLE tenant_management_classifications CHANGE COLUMN catalog_id id_catalogo BIGINT NOT NULL;
ALTER TABLE tenant_management_classifications CHANGE COLUMN is_enabled esta_habilitado BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE tenant_management_classifications CHANGE COLUMN custom_label etiqueta_personalizada VARCHAR(255);
ALTER TABLE tenant_management_classifications CHANGE COLUMN requires_payment_override requiere_pago_personalizado BOOLEAN;
ALTER TABLE tenant_management_classifications CHANGE COLUMN requires_schedule_override requiere_cronograma_personalizado BOOLEAN;
ALTER TABLE tenant_management_classifications CHANGE COLUMN requires_follow_up_override requiere_seguimiento_personalizado BOOLEAN;
ALTER TABLE tenant_management_classifications CHANGE COLUMN requires_installment_plan_override requiere_plan_cuotas_personalizado BOOLEAN;
ALTER TABLE tenant_management_classifications CHANGE COLUMN display_order orden_visualizacion INT;
ALTER TABLE tenant_management_classifications CHANGE COLUMN config_json configuracion_json JSON;
ALTER TABLE tenant_management_classifications CHANGE COLUMN created_at fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE tenant_management_classifications CHANGE COLUMN updated_at fecha_actualizacion DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE tenant_management_classifications DROP FOREIGN KEY tenant_management_classifications_ibfk_1;
ALTER TABLE tenant_management_classifications DROP FOREIGN KEY tenant_management_classifications_ibfk_2;
ALTER TABLE tenant_management_classifications DROP FOREIGN KEY tenant_management_classifications_ibfk_3;

DROP INDEX idx_tenant_mgmt_unique ON tenant_management_classifications;
DROP INDEX idx_tenant_mgmt_tenant ON tenant_management_classifications;
DROP INDEX idx_tenant_mgmt_portfolio ON tenant_management_classifications;
DROP INDEX idx_tenant_mgmt_catalog ON tenant_management_classifications;

ALTER TABLE tenant_management_classifications ADD CONSTRAINT fk_gestion_inquilino
    FOREIGN KEY (id_inquilino) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE tenant_management_classifications ADD CONSTRAINT fk_gestion_cartera
    FOREIGN KEY (id_cartera) REFERENCES portfolios(id) ON DELETE CASCADE;
ALTER TABLE tenant_management_classifications ADD CONSTRAINT fk_gestion_catalogo
    FOREIGN KEY (id_catalogo) REFERENCES management_classification_catalog(id) ON DELETE CASCADE;

CREATE UNIQUE INDEX idx_gestion_unico ON tenant_management_classifications(id_inquilino, id_cartera, id_catalogo);
CREATE INDEX idx_gestion_inquilino ON tenant_management_classifications(id_inquilino);
CREATE INDEX idx_gestion_cartera ON tenant_management_classifications(id_cartera);
CREATE INDEX idx_gestion_catalogo ON tenant_management_classifications(id_catalogo);

-- ========================================
-- PARTE 9: MANAGEMENTS TABLE
-- ========================================

ALTER TABLE managements CHANGE COLUMN tenant_id id_inquilino BIGINT;
ALTER TABLE managements CHANGE COLUMN portfolio_id id_cartera BIGINT;
ALTER TABLE managements CHANGE COLUMN campaign_id id_campana BIGINT;
ALTER TABLE managements CHANGE COLUMN legacy_campaign_id id_campana_legacy VARCHAR(255);
ALTER TABLE managements CHANGE COLUMN customer_id id_cliente VARCHAR(255);
ALTER TABLE managements CHANGE COLUMN advisor_id id_asesor VARCHAR(255);
ALTER TABLE managements CHANGE COLUMN management_date fecha_gestion DATETIME;
ALTER TABLE managements CHANGE COLUMN management_type tipo_gestion VARCHAR(50);
ALTER TABLE managements CHANGE COLUMN contact_result resultado_contacto VARCHAR(50);
ALTER TABLE managements CHANGE COLUMN payment_amount monto_pago DECIMAL(15,2);
ALTER TABLE managements CHANGE COLUMN payment_date fecha_pago DATE;
ALTER TABLE managements CHANGE COLUMN promise_date fecha_promesa DATE;
ALTER TABLE managements CHANGE COLUMN promise_amount monto_promesa DECIMAL(15,2);
ALTER TABLE managements CHANGE COLUMN follow_up_date fecha_seguimiento DATE;
ALTER TABLE managements CHANGE COLUMN notes notas TEXT;

-- Recrear foreign keys e índices
ALTER TABLE managements DROP FOREIGN KEY IF EXISTS fk_mgmt_tenant;
ALTER TABLE managements DROP FOREIGN KEY IF EXISTS fk_mgmt_portfolio;
ALTER TABLE managements DROP FOREIGN KEY IF EXISTS fk_mgmt_campaign;

DROP INDEX IF EXISTS idx_mgmt_tenant ON managements;
DROP INDEX IF EXISTS idx_mgmt_portfolio ON managements;
DROP INDEX IF EXISTS idx_mgmt_campaign ON managements;
DROP INDEX IF EXISTS idx_mgmt_customer ON managements;
DROP INDEX IF EXISTS idx_mgmt_advisor ON managements;
DROP INDEX IF EXISTS idx_mgmt_date ON managements;

ALTER TABLE managements ADD CONSTRAINT fk_gestion_inquilino
    FOREIGN KEY (id_inquilino) REFERENCES tenants(id) ON DELETE RESTRICT;
ALTER TABLE managements ADD CONSTRAINT fk_gestion_cartera
    FOREIGN KEY (id_cartera) REFERENCES portfolios(id) ON DELETE SET NULL;
ALTER TABLE managements ADD CONSTRAINT fk_gestion_campana
    FOREIGN KEY (id_campana) REFERENCES campaigns(id) ON DELETE SET NULL;

CREATE INDEX idx_gestion_inquilino ON managements(id_inquilino);
CREATE INDEX idx_gestion_cartera ON managements(id_cartera);
CREATE INDEX idx_gestion_campana ON managements(id_campana);
CREATE INDEX idx_gestion_cliente ON managements(id_cliente);
CREATE INDEX idx_gestion_asesor ON managements(id_asesor);
CREATE INDEX idx_gestion_fecha ON managements(fecha_gestion);

-- ========================================
-- PARTE 10: MANAGEMENT_DYNAMIC_FIELDS
-- ========================================

ALTER TABLE management_dynamic_fields CHANGE COLUMN management_id id_gestion BIGINT NOT NULL;
ALTER TABLE management_dynamic_fields CHANGE COLUMN field_definition_id id_definicion_campo BIGINT NOT NULL;
ALTER TABLE management_dynamic_fields CHANGE COLUMN field_value valor_campo TEXT;
ALTER TABLE management_dynamic_fields CHANGE COLUMN field_value_numeric valor_campo_numerico DECIMAL(20,6);
ALTER TABLE management_dynamic_fields CHANGE COLUMN field_value_date valor_campo_fecha DATETIME;
ALTER TABLE management_dynamic_fields CHANGE COLUMN field_value_boolean valor_campo_booleano BOOLEAN;
ALTER TABLE management_dynamic_fields CHANGE COLUMN field_value_json valor_campo_json JSON;
ALTER TABLE management_dynamic_fields CHANGE COLUMN created_at fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE management_dynamic_fields CHANGE COLUMN updated_at fecha_actualizacion DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE management_dynamic_fields DROP FOREIGN KEY management_dynamic_fields_ibfk_1;
ALTER TABLE management_dynamic_fields DROP FOREIGN KEY management_dynamic_fields_ibfk_2;

DROP INDEX idx_mgmt_dynamic_unique ON management_dynamic_fields;
DROP INDEX idx_mgmt_dynamic_management ON management_dynamic_fields;
DROP INDEX idx_mgmt_dynamic_field ON management_dynamic_fields;

ALTER TABLE management_dynamic_fields ADD CONSTRAINT fk_campo_dinamico_gestion
    FOREIGN KEY (id_gestion) REFERENCES managements(id) ON DELETE CASCADE;
ALTER TABLE management_dynamic_fields ADD CONSTRAINT fk_campo_dinamico_definicion
    FOREIGN KEY (id_definicion_campo) REFERENCES field_definitions(id) ON DELETE CASCADE;

CREATE UNIQUE INDEX idx_campo_dinamico_unico ON management_dynamic_fields(id_gestion, id_definicion_campo);
CREATE INDEX idx_campo_dinamico_gestion ON management_dynamic_fields(id_gestion);
CREATE INDEX idx_campo_dinamico_campo ON management_dynamic_fields(id_definicion_campo);

-- ========================================
-- PARTE 11: BUSINESS RULES & STRATEGIES
-- ========================================

ALTER TABLE tenant_business_rules CHANGE COLUMN tenant_id id_inquilino BIGINT NOT NULL;
ALTER TABLE tenant_business_rules CHANGE COLUMN portfolio_id id_cartera BIGINT;
ALTER TABLE tenant_business_rules CHANGE COLUMN rule_code codigo_regla VARCHAR(100) NOT NULL;
ALTER TABLE tenant_business_rules CHANGE COLUMN rule_name nombre_regla VARCHAR(255) NOT NULL;
ALTER TABLE tenant_business_rules CHANGE COLUMN rule_type tipo_regla VARCHAR(50) NOT NULL;
ALTER TABLE tenant_business_rules CHANGE COLUMN rule_category categoria_regla VARCHAR(100);
ALTER TABLE tenant_business_rules CHANGE COLUMN description descripcion TEXT;
ALTER TABLE tenant_business_rules CHANGE COLUMN is_active esta_activo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE tenant_business_rules CHANGE COLUMN priority prioridad INT;
ALTER TABLE tenant_business_rules CHANGE COLUMN condition_expression expresion_condicion TEXT;
ALTER TABLE tenant_business_rules CHANGE COLUMN action_expression expresion_accion TEXT;
ALTER TABLE tenant_business_rules CHANGE COLUMN validation_json validacion_json JSON;
ALTER TABLE tenant_business_rules CHANGE COLUMN error_message mensaje_error VARCHAR(500);
ALTER TABLE tenant_business_rules CHANGE COLUMN created_at fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE tenant_business_rules CHANGE COLUMN updated_at fecha_actualizacion DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE tenant_business_rules DROP FOREIGN KEY tenant_business_rules_ibfk_1;
ALTER TABLE tenant_business_rules DROP FOREIGN KEY tenant_business_rules_ibfk_2;

DROP INDEX idx_rule_unique ON tenant_business_rules;
DROP INDEX idx_rule_tenant ON tenant_business_rules;
DROP INDEX idx_rule_portfolio ON tenant_business_rules;
DROP INDEX idx_rule_code ON tenant_business_rules;

ALTER TABLE tenant_business_rules ADD CONSTRAINT fk_regla_inquilino
    FOREIGN KEY (id_inquilino) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE tenant_business_rules ADD CONSTRAINT fk_regla_cartera
    FOREIGN KEY (id_cartera) REFERENCES portfolios(id) ON DELETE CASCADE;

CREATE UNIQUE INDEX idx_regla_unica ON tenant_business_rules(id_inquilino, id_cartera, codigo_regla);
CREATE INDEX idx_regla_inquilino ON tenant_business_rules(id_inquilino);
CREATE INDEX idx_regla_cartera ON tenant_business_rules(id_cartera);
CREATE INDEX idx_regla_codigo ON tenant_business_rules(codigo_regla);

ALTER TABLE processing_strategies CHANGE COLUMN tenant_id id_inquilino BIGINT NOT NULL;
ALTER TABLE processing_strategies CHANGE COLUMN portfolio_id id_cartera BIGINT;
ALTER TABLE processing_strategies CHANGE COLUMN strategy_type tipo_estrategia VARCHAR(100) NOT NULL;
ALTER TABLE processing_strategies CHANGE COLUMN strategy_implementation implementacion_estrategia VARCHAR(500) NOT NULL;
ALTER TABLE processing_strategies CHANGE COLUMN strategy_name nombre_estrategia VARCHAR(255);
ALTER TABLE processing_strategies CHANGE COLUMN description descripcion TEXT;
ALTER TABLE processing_strategies CHANGE COLUMN is_active esta_activo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE processing_strategies CHANGE COLUMN priority prioridad INT;
ALTER TABLE processing_strategies CHANGE COLUMN config_json configuracion_json JSON;
ALTER TABLE processing_strategies CHANGE COLUMN created_at fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE processing_strategies CHANGE COLUMN updated_at fecha_actualizacion DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE processing_strategies DROP FOREIGN KEY processing_strategies_ibfk_1;
ALTER TABLE processing_strategies DROP FOREIGN KEY processing_strategies_ibfk_2;

DROP INDEX idx_strategy_unique ON processing_strategies;
DROP INDEX idx_strategy_tenant ON processing_strategies;
DROP INDEX idx_strategy_portfolio ON processing_strategies;
DROP INDEX idx_strategy_type ON processing_strategies;

ALTER TABLE processing_strategies ADD CONSTRAINT fk_estrategia_inquilino
    FOREIGN KEY (id_inquilino) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE processing_strategies ADD CONSTRAINT fk_estrategia_cartera
    FOREIGN KEY (id_cartera) REFERENCES portfolios(id) ON DELETE CASCADE;

CREATE UNIQUE INDEX idx_estrategia_unica ON processing_strategies(id_inquilino, id_cartera, tipo_estrategia);
CREATE INDEX idx_estrategia_inquilino ON processing_strategies(id_inquilino);
CREATE INDEX idx_estrategia_cartera ON processing_strategies(id_cartera);
CREATE INDEX idx_estrategia_tipo ON processing_strategies(tipo_estrategia);

-- ========================================
-- FIN DE LA MIGRACIÓN V3
-- ========================================
