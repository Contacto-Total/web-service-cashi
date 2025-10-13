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
 * FieldDefinition Entity - Master catalog of available dynamic fields
 * Defines all possible fields that can be enabled per tenant
 */
@Entity
@Table(name = "definiciones_campos", indexes = {
    @Index(name = "idx_codigo_campo", columnList = "codigo_campo", unique = true),
    @Index(name = "idx_categoria_campo", columnList = "categoria_campo")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class FieldDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_campo", unique = true, nullable = false, length = 100)
    private String fieldCode;

    @Column(name = "nombre_campo", nullable = false, length = 255)
    private String fieldName;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_campo", nullable = false, length = 50)
    private FieldType fieldType;

    @Column(name = "categoria_campo", length = 100)
    private String fieldCategory;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String description;

    @Column(name = "valor_predeterminado", length = 500)
    private String defaultValue;

    @Column(name = "reglas_validacion", columnDefinition = "JSON")
    private String validationRules;

    @Column(name = "orden_visualizacion")
    private Integer displayOrder;

    @Column(name = "es_campo_sistema", nullable = false)
    private Boolean isSystemField = false;

    @CreatedDate
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "fecha_actualizacion")
    private LocalDateTime updatedAt;

    public enum FieldType {
        TEXT,
        NUMBER,
        DECIMAL,
        DATE,
        DATETIME,
        BOOLEAN,
        SELECT,
        MULTI_SELECT,
        JSON,
        PHONE,
        EMAIL,
        URL,
        CURRENCY
    }

    public FieldDefinition(String fieldCode, String fieldName, FieldType fieldType, String fieldCategory) {
        this.fieldCode = fieldCode;
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.fieldCategory = fieldCategory;
        this.isSystemField = false;
    }

    public FieldDefinition(String fieldCode, String fieldName, FieldType fieldType,
                          String fieldCategory, String description, Boolean isSystemField) {
        this.fieldCode = fieldCode;
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.fieldCategory = fieldCategory;
        this.description = description;
        this.isSystemField = isSystemField != null ? isSystemField : false;
    }
}
