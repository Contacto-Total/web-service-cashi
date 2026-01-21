package com.cashi.shared.domain.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;

/**
 * FieldDefinition Entity - Catálogo maestro de campos del sistema
 * Define el vocabulario estándar que todo el sistema utiliza
 * Se mapean a través de HeaderConfiguration a las columnas dinámicas por subcartera
 */
@Entity
@Table(name = "definiciones_campos", indexes = {
    @Index(name = "idx_field_code", columnList = "codigo_campo", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class FieldDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Código único del campo (ej: documento, monto_capital, fecha_vencimiento)
     */
    @Column(name = "codigo_campo", nullable = false, unique = true, length = 100)
    private String fieldCode;

    /**
     * Nombre descriptivo del campo
     */
    @Column(name = "nombre_campo", nullable = false, length = 255)
    private String fieldName;

    /**
     * Descripción del propósito del campo
     */
    @Column(name = "descripcion", length = 500)
    private String description;

    /**
     * Tipo de dato del campo (TEXTO, NUMERICO, FECHA, BOOLEANO)
     */
    @Column(name = "tipo_dato", nullable = false, length = 20)
    private String dataType;

    /**
     * Formato del campo (ej: "decimal(18,2)", "dd/MM/yyyy")
     */
    @Column(name = "formato", length = 100)
    private String format;

    /**
     * Tabla asociada al campo (ej: "clientes", "metodos_contacto")
     * Indica a qué tabla de la base de datos pertenece este campo
     */
    @Column(name = "tabla_asociada", length = 100)
    private String associatedTable;

    @CreatedDate
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDate createdAt;

    @LastModifiedDate
    @Column(name = "fecha_actualizacion")
    private LocalDate updatedAt;

    /**
     * Constructor para seeding (sin tabla asociada - retrocompatibilidad)
     */
    public FieldDefinition(String fieldCode, String fieldName, String description,
                          String dataType, String format) {
        this.fieldCode = fieldCode;
        this.fieldName = fieldName;
        this.description = description;
        this.dataType = dataType;
        this.format = format;
    }

    /**
     * Constructor completo con tabla asociada
     */
    public FieldDefinition(String fieldCode, String fieldName, String description,
                          String dataType, String format, String associatedTable) {
        this.fieldCode = fieldCode;
        this.fieldName = fieldName;
        this.description = description;
        this.dataType = dataType;
        this.format = format;
        this.associatedTable = associatedTable;
    }
}
