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
import java.util.ArrayList;
import java.util.List;

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
     * Valores: TEXTO, NUMERICO, FECHA, BOOLEANO
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

    // ========== CAMPOS DE TRANSFORMACIÓN ==========

    /**
     * Campo origen desde el cual se extraerá el valor mediante regex
     * Ejemplo: "IDENTITY_CODE" si documento se deriva de IDENTITY_CODE
     * Si es NULL, el valor viene directamente del CSV con el nombre de headerName
     */
    @Column(name = "campo_origen", length = 100)
    private String sourceField;

    /**
     * Patrón regex para extraer/transformar el valor del campo origen
     * Ejemplo: ".{8}$" para extraer últimos 8 caracteres
     * Ejemplo: "(?<=D)(.{8})" para extraer 8 caracteres después de "D"
     * Si es NULL y sourceField tiene valor, se copia tal cual
     */
    @Column(name = "patron_regex", length = 500)
    private String regexPattern;

    @CreatedDate
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDate createdAt;

    @LastModifiedDate
    @Column(name = "fecha_actualizacion")
    private LocalDate updatedAt;

    // ========== CAMPOS PARA SISTEMA DE ALIAS Y AUTO-DETECCIÓN ==========

    /**
     * Lista de alias (nombres alternativos) para esta cabecera
     * Permite que la cabecera sea reconocida por múltiples nombres en los archivos Excel
     */
    @OneToMany(mappedBy = "headerConfiguration", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<HeaderAlias> aliases = new ArrayList<>();

    /**
     * Indica si se deben agregar automáticamente columnas nuevas detectadas
     * 1 = agregar automáticamente como tipo TEXTO
     * 0 = preguntar al usuario (default)
     */
    @Column(name = "auto_agregar_nuevas")
    private Integer autoAddNewColumns = 0;

    /**
     * Lista de nombres de columnas a ignorar en la importación (JSON array)
     * Estas columnas no generarán error si aparecen en el Excel
     */
    @Column(name = "columnas_ignoradas", columnDefinition = "JSON")
    private String ignoredColumns;

    /**
     * Agrega un alias a esta cabecera
     */
    public void addAlias(String aliasName, boolean isPrincipal) {
        HeaderAlias alias = new HeaderAlias(this, aliasName, isPrincipal);
        this.aliases.add(alias);
    }

    /**
     * Obtiene todos los nombres válidos (nombre principal + alias)
     */
    public List<String> getAllValidNames() {
        List<String> names = new ArrayList<>();
        names.add(this.headerName); // Nombre principal
        for (HeaderAlias alias : aliases) {
            if (!alias.getAlias().equalsIgnoreCase(this.headerName)) {
                names.add(alias.getAlias());
            }
        }
        return names;
    }

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
