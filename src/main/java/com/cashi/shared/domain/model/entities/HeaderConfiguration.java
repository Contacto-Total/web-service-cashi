package com.cashi.shared.domain.model.entities;

import com.cashi.shared.domain.model.valueobjects.LoadType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;

/**
 * HeaderConfiguration Entity - Configuración de cabeceras personalizadas por subcartera
 * Traduce el "lenguaje del proveedor" al "lenguaje del sistema" mediante el catálogo
 */
@Entity
@Table(name = "configuracion_cabeceras", indexes = {
    @Index(name = "idx_header_config_subportfolio", columnList = "id_subcartera"),
    @Index(name = "idx_header_config_field_def", columnList = "id_definicion_campo"),
    @Index(name = "idx_header_config_unique", columnList = "id_subcartera, nombre_cabecera, tipo_carga", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class HeaderConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_subcartera", nullable = false)
    private SubPortfolio subPortfolio;

    /**
     * Referencia al catálogo maestro de campos del sistema
     * Puede ser NULL para campos personalizados que no están en el catálogo
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "id_definicion_campo", nullable = true)
    private FieldDefinition fieldDefinition;

    /**
     * Nombre de la cabecera tal como viene del proveedor (ej: "DNI", "Saldo Vencido")
     */
    @Column(name = "nombre_cabecera", nullable = false, length = 100)
    private String headerName;

    /**
     * Tipo de dato (heredado del catálogo pero almacenado por performance)
     * Valores: TEXTO, NUMERICO, FECHA
     */
    @Column(name = "tipo_dato", nullable = false, length = 20)
    private String dataType;

    /**
     * Etiqueta visual para mostrar en la UI
     */
    @Column(name = "etiqueta_visual", nullable = false, length = 255)
    private String displayLabel;

    /**
     * Formato específico para esta subcartera (puede diferir del formato del sistema)
     */
    @Column(name = "formato", length = 100)
    private String format;

    /**
     * Indica si el campo es obligatorio para esta subcartera
     */
    @Column(name = "obligatorio", nullable = false)
    private Integer required = 0;

    /**
     * Tipo de carga: INICIAL (carga inicial del mes) o ACTUALIZACION (carga diaria)
     * Determina el prefijo de la tabla dinámica
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_carga", nullable = false, length = 20)
    private LoadType loadType = LoadType.ACTUALIZACION;

    @CreatedDate
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDate createdAt;

    @LastModifiedDate
    @Column(name = "fecha_actualizacion")
    private LocalDate updatedAt;

    /**
     * Constructor para crear configuración vinculada al catálogo
     */
    public HeaderConfiguration(SubPortfolio subPortfolio, FieldDefinition fieldDefinition,
                              String headerName, String displayLabel, String format, Integer required, LoadType loadType) {
        this.subPortfolio = subPortfolio;
        this.fieldDefinition = fieldDefinition;
        this.headerName = headerName;
        this.displayLabel = displayLabel;
        this.format = format;
        this.required = required != null ? required : 0;
        this.loadType = loadType != null ? loadType : LoadType.ACTUALIZACION;
        // Heredar tipo de dato del catálogo
        this.dataType = fieldDefinition.getDataType();
    }

    /**
     * Constructor para crear configuración de campo personalizado (sin vínculo al catálogo)
     */
    public HeaderConfiguration(SubPortfolio subPortfolio, String headerName, String dataType,
                              String displayLabel, String format, Integer required, LoadType loadType) {
        this.subPortfolio = subPortfolio;
        this.fieldDefinition = null; // Campo personalizado
        this.headerName = headerName;
        this.dataType = dataType;
        this.displayLabel = displayLabel;
        this.format = format;
        this.required = required != null ? required : 0;
        this.loadType = loadType != null ? loadType : LoadType.ACTUALIZACION;
    }
}
