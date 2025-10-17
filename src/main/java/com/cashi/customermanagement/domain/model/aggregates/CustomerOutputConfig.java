package com.cashi.customermanagement.domain.model.aggregates;

import com.cashi.shared.domain.AggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Configuración de Outputs del Cliente
 *
 * Define qué campos del cliente se mostrarán en la pantalla de gestión de cobranzas
 * y cómo se mostrarán (orden, formato, destacados, etc.)
 *
 * RELACIÓN CON ENTIDADES:
 * - Configuración específica por tenant (financiera)
 * - Opcionalmente por portfolio (cartera)
 * - Los campos configurados hacen referencia a:
 *   - Customer.java (datos personales)
 *   - ContactInfo.java (información de contacto)
 *   - DebtInfo.java (información de deuda)
 *   - AccountInfo.java (información de cuenta)
 *
 * FLUJO DE USO:
 * 1. Administrador configura outputs en: /maintenance/customer-outputs
 * 2. Configuración se guarda en esta tabla
 * 3. Pantalla de gestión carga configuración y muestra campos según config
 */
@Entity
@Table(name = "customer_output_config")
@Getter
@NoArgsConstructor
public class CustomerOutputConfig extends AggregateRoot {

    /**
     * ID del tenant (financiera) al que pertenece esta configuración
     * FK a tabla: tenants
     */
    @Column(name = "id_inquilino", nullable = false)
    private Long tenantId;

    /**
     * ID del portfolio (cartera) al que pertenece esta configuración
     * FK a tabla: portfolios
     * NULL = Configuración aplica a todas las carteras del tenant
     */
    @Column(name = "id_cartera")
    private Long portfolioId;

    /**
     * Configuración de campos en formato JSON
     *
     * Estructura JSON:
     * [
     *   {
     *     "id": "documentCode",
     *     "label": "DNI/Documento",
     *     "field": "documentCode",
     *     "category": "personal",
     *     "format": "text",
     *     "isVisible": true,
     *     "displayOrder": 1,
     *     "highlight": false
     *   },
     *   ...
     * ]
     *
     * Campos:
     * - id: Identificador único del campo
     * - label: Etiqueta a mostrar en UI
     * - field: Path al campo en el objeto Customer (ej: "contactInfo.mobilePhone")
     * - category: Categoría ("personal", "contact", "debt", "account")
     * - format: Formato de visualización ("text", "currency", "number", "date")
     * - isVisible: Si se muestra o no
     * - displayOrder: Orden de visualización
     * - highlight: Si se destaca visualmente
     */
    @Column(name = "configuracion_campos", columnDefinition = "TEXT", nullable = false)
    private String fieldsConfig;

    /**
     * Constructor para crear nueva configuración
     */
    public CustomerOutputConfig(Long tenantId, Long portfolioId, String fieldsConfig) {
        super();
        this.tenantId = tenantId;
        this.portfolioId = portfolioId;
        this.fieldsConfig = fieldsConfig;
    }

    /**
     * Actualiza la configuración de campos
     */
    public void updateFieldsConfig(String fieldsConfig) {
        this.fieldsConfig = fieldsConfig;
        updateTimestamp();
    }

    /**
     * Actualiza el portfolio asociado
     */
    public void updatePortfolio(Long portfolioId) {
        this.portfolioId = portfolioId;
        updateTimestamp();
    }
}
