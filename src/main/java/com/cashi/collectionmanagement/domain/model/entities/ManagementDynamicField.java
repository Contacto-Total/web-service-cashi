package com.cashi.collectionmanagement.domain.model.entities;

import com.cashi.collectionmanagement.domain.model.aggregates.Management;
import com.cashi.shared.domain.model.entities.FieldDefinition;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * ManagementDynamicField Entity - EAV pattern implementation
 * Stores tenant-specific dynamic field values for managements
 */
@Entity
@Table(name = "campos_dinamicos_gestion", indexes = {
    @Index(name = "idx_campo_din_gestion", columnList = "id_gestion"),
    @Index(name = "idx_campo_din_campo", columnList = "id_definicion_campo"),
    @Index(name = "idx_campo_din_unico", columnList = "id_gestion, id_definicion_campo", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class ManagementDynamicField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_gestion", nullable = false)
    private Management management;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_definicion_campo", nullable = false)
    private FieldDefinition fieldDefinition;

    @Column(name = "valor_campo", columnDefinition = "TEXT")
    private String fieldValue;

    @Column(name = "valor_campo_numerico")
    private java.math.BigDecimal fieldValueNumeric;

    @Column(name = "valor_campo_fecha")
    private LocalDateTime fieldValueDate;

    @Column(name = "valor_campo_booleano")
    private Boolean fieldValueBoolean;

    @Column(name = "valor_campo_json", columnDefinition = "JSON")
    private String fieldValueJson;

    @CreatedDate
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "fecha_actualizacion")
    private LocalDateTime updatedAt;

    public ManagementDynamicField(Management management, FieldDefinition fieldDefinition, String fieldValue) {
        this.management = management;
        this.fieldDefinition = fieldDefinition;
        this.fieldValue = fieldValue;
    }

    public ManagementDynamicField(Management management, FieldDefinition fieldDefinition,
                                 String fieldValue, java.math.BigDecimal fieldValueNumeric,
                                 LocalDateTime fieldValueDate, Boolean fieldValueBoolean) {
        this.management = management;
        this.fieldDefinition = fieldDefinition;
        this.fieldValue = fieldValue;
        this.fieldValueNumeric = fieldValueNumeric;
        this.fieldValueDate = fieldValueDate;
        this.fieldValueBoolean = fieldValueBoolean;
    }

    /**
     * Gets the appropriate value based on field type
     */
    public Object getTypedValue() {
        return switch (fieldDefinition.getFieldType()) {
            case NUMBER, DECIMAL, CURRENCY -> fieldValueNumeric;
            case DATE, DATETIME -> fieldValueDate;
            case BOOLEAN -> fieldValueBoolean;
            case JSON, MULTI_SELECT -> fieldValueJson;
            default -> fieldValue;
        };
    }

    /**
     * Sets the value based on field type
     */
    public void setTypedValue(Object value) {
        if (value == null) {
            this.fieldValue = null;
            this.fieldValueNumeric = null;
            this.fieldValueDate = null;
            this.fieldValueBoolean = null;
            this.fieldValueJson = null;
            return;
        }

        switch (fieldDefinition.getFieldType()) {
            case NUMBER, DECIMAL, CURRENCY -> {
                if (value instanceof Number) {
                    this.fieldValueNumeric = new java.math.BigDecimal(value.toString());
                }
            }
            case DATE, DATETIME -> {
                if (value instanceof LocalDateTime) {
                    this.fieldValueDate = (LocalDateTime) value;
                }
            }
            case BOOLEAN -> {
                if (value instanceof Boolean) {
                    this.fieldValueBoolean = (Boolean) value;
                }
            }
            case JSON, MULTI_SELECT -> {
                this.fieldValueJson = value.toString();
            }
            default -> {
                this.fieldValue = value.toString();
            }
        }
    }
}
