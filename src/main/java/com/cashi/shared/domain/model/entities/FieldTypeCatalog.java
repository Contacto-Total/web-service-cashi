package com.cashi.shared.domain.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Catálogo de tipos de campo disponibles para formularios dinámicos.
 * Define los tipos que los usuarios pueden usar al configurar campos personalizados.
 */
@Entity
@Table(name = "catalogo_tipos_campo")
@Getter
@Setter
@NoArgsConstructor
public class FieldTypeCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Código único del tipo (ej: text, number, currency, table)
     */
    @Column(name = "codigo_tipo", nullable = false, unique = true, length = 50)
    private String typeCode;

    /**
     * Nombre legible del tipo
     */
    @Column(name = "nombre_tipo", nullable = false, length = 100)
    private String typeName;

    /**
     * Descripción de qué hace este tipo de campo
     */
    @Column(name = "descripcion", length = 500)
    private String description;

    /**
     * Icono de Lucide para mostrar en la UI
     */
    @Column(name = "icono", length = 50)
    private String icon;

    /**
     * Indica si este tipo está disponible para campos principales
     * (algunos tipos como auto-number solo son para columnas de tabla)
     */
    @Column(name = "disponible_campo_principal", nullable = false)
    private Boolean availableForMainField = true;

    /**
     * Indica si este tipo está disponible como columna de tabla
     */
    @Column(name = "disponible_columna_tabla", nullable = false)
    private Boolean availableForTableColumn = true;

    /**
     * Orden de visualización en la UI
     */
    @Column(name = "orden_visualizacion")
    private Integer displayOrder = 0;

    /**
     * Indica si el tipo está activo
     */
    @Column(name = "activo", nullable = false)
    private Boolean isActive = true;

    /**
     * Configuración adicional en formato JSON (opcional)
     * Por ejemplo: validaciones default, opciones de renderizado, etc.
     */
    @Column(name = "configuracion_json", columnDefinition = "TEXT")
    private String configJson;

    public FieldTypeCatalog(String typeCode, String typeName, String description, String icon,
                           Boolean availableForMainField, Boolean availableForTableColumn, Integer displayOrder) {
        this.typeCode = typeCode;
        this.typeName = typeName;
        this.description = description;
        this.icon = icon;
        this.availableForMainField = availableForMainField;
        this.availableForTableColumn = availableForTableColumn;
        this.displayOrder = displayOrder;
        this.isActive = true;
    }
}
