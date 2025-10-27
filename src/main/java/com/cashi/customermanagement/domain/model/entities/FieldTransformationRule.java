package com.cashi.customermanagement.domain.model.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Regla de transformación de campos para importación de clientes
 * Permite derivar campos como "documento" desde otros campos como "codigo_identificacion"
 */
@Entity
@Table(name = "reglas_configuracion_cabeceras")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldTransformationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID del tenant al que pertenece esta regla
     */
    @Column(name = "id_inquilino", nullable = false)
    private Long tenantId;

    /**
     * ID de la subcartera donde se aplicará esta regla
     */
    @Column(name = "id_subcartera")
    private Integer subPortfolioId;

    /**
     * Campo destino que se va a generar (ej: "documento")
     */
    @Column(name = "campo_destino", nullable = false)
    private String targetField;

    /**
     * Campo origen desde el cual se deriva (ej: "codigo_identificacion")
     */
    @Column(name = "campo_origen", nullable = false)
    private String sourceField;

    /**
     * Nombre de la nueva cabecera a crear (ej: "DNI", "RUC")
     */
    @Column(name = "nombre_cabecera_destino")
    private String targetHeaderName;

    /**
     * Prefijo que debe tener el valor origen para aplicar esta regla (ej: "D", "C")
     */
    @Column(name = "prefijo_inicio")
    private String startsWithPrefix;

    /**
     * Número de caracteres a extraer desde el final (ej: 8 para DNI, 7 para CE)
     */
    @Column(name = "extraer_ultimos_n_caracteres")
    private Integer extractLastNChars;

    /**
     * Patrón regex opcional para extracciones más complejas
     */
    @Column(name = "patron_regex", length = 500)
    private String regexPattern;

    /**
     * Grupo de captura del regex (por defecto 1)
     */
    @Column(name = "grupo_captura_regex")
    private Integer regexCaptureGroup;

    /**
     * Orden de aplicación de las reglas (menor = mayor prioridad)
     */
    @Column(name = "orden_regla")
    private Integer ruleOrder;

    /**
     * Indica si la regla está activa
     */
    @Column(name = "activo")
    private Boolean isActive = true;

    /**
     * Descripción de la regla para facilitar su gestión
     */
    @Column(name = "descripcion")
    private String description;
}
