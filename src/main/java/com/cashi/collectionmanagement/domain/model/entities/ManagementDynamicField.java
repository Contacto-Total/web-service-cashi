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
@Table(name = "management_dynamic_fields", indexes = {
    @Index(name = "idx_mgmt_dynamic_management", columnList = "management_id"),
    @Index(name = "idx_mgmt_dynamic_field", columnList = "field_definition_id"),
    @Index(name = "idx_mgmt_dynamic_unique", columnList = "management_id, field_definition_id", unique = true)
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
    @JoinColumn(name = "management_id", nullable = false)
    private Management management;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "field_definition_id", nullable = false)
    private FieldDefinition fieldDefinition;

    @Column(name = "field_value", columnDefinition = "TEXT")
    private String fieldValue;

    @Column(name = "field_value_numeric")
    private java.math.BigDecimal fieldValueNumeric;

    @Column(name = "field_value_date")
    private LocalDateTime fieldValueDate;

    @Column(name = "field_value_boolean")
    private Boolean fieldValueBoolean;

    @Column(name = "field_value_json", columnDefinition = "JSON")
    private String fieldValueJson;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
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
