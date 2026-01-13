package com.cashi.shared.domain.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * ComplementaryFileType Entity - Tipos de archivos complementarios por subcartera
 * Define archivos adicionales que actualizan columnas específicas de registros existentes
 */
@Entity
@Table(name = "tipos_archivo_complementario", indexes = {
    @Index(name = "idx_tipo_archivo_subcartera", columnList = "id_subcartera"),
    @Index(name = "idx_tipo_archivo_unico", columnList = "id_subcartera, nombre_tipo", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class ComplementaryFileType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Subcartera a la que pertenece este tipo de archivo
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_subcartera", nullable = false)
    private SubPortfolio subPortfolio;

    /**
     * Nombre identificador del tipo de archivo
     * Ejemplo: "FACILIDADES", "PKM"
     */
    @Column(name = "nombre_tipo", nullable = false, length = 50)
    private String typeName;

    /**
     * Patrón regex para identificar el archivo por su nombre
     * Ejemplo: "Facilidades_Pago_CONTACTO_TOTAL_.*\\.xlsx"
     */
    @Column(name = "patron_nombre", nullable = false, length = 200)
    private String fileNamePattern;

    /**
     * Campo utilizado para vincular registros (hacer match)
     * Ejemplo: "IDENTITY_CODE", "NUM_CUENTA_PMCP"
     */
    @Column(name = "campo_vinculacion", nullable = false, length = 100)
    private String linkField;

    /**
     * Lista de columnas que este archivo actualiza (JSON array)
     * Ejemplo: ["FLAG_CONVENIO", "FLAG_LTD"]
     */
    @Column(name = "columnas_actualizar", nullable = false, columnDefinition = "JSON")
    private String columnsToUpdate;

    /**
     * Descripción del tipo de archivo
     */
    @Column(name = "descripcion", length = 255)
    private String description;

    /**
     * Estado activo/inactivo
     */
    @Column(name = "esta_activo", nullable = false)
    private Integer isActive = 1;

    @CreatedDate
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "fecha_actualizacion")
    private LocalDateTime updatedAt;

    /**
     * Constructor completo
     */
    public ComplementaryFileType(SubPortfolio subPortfolio, String typeName, String fileNamePattern,
                                  String linkField, String columnsToUpdate, String description) {
        this.subPortfolio = subPortfolio;
        this.typeName = typeName;
        this.fileNamePattern = fileNamePattern;
        this.linkField = linkField;
        this.columnsToUpdate = columnsToUpdate;
        this.description = description;
        this.isActive = 1;
    }

    /**
     * Verifica si el tipo está activo
     */
    public boolean isActive() {
        return isActive != null && isActive == 1;
    }
}
