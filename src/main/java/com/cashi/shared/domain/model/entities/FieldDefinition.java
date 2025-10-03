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
@Table(name = "field_definitions", indexes = {
    @Index(name = "idx_field_code", columnList = "field_code", unique = true),
    @Index(name = "idx_field_category", columnList = "field_category")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class FieldDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "field_code", unique = true, nullable = false, length = 100)
    private String fieldCode;

    @Column(name = "field_name", nullable = false, length = 255)
    private String fieldName;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false, length = 50)
    private FieldType fieldType;

    @Column(name = "field_category", length = 100)
    private String fieldCategory;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "default_value", length = 500)
    private String defaultValue;

    @Column(name = "validation_rules", columnDefinition = "JSON")
    private String validationRules;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "is_system_field", nullable = false)
    private Boolean isSystemField = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
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
