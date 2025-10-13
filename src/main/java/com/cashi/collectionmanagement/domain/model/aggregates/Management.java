package com.cashi.collectionmanagement.domain.model.aggregates;

import com.cashi.collectionmanagement.domain.model.entities.CallDetail;
import com.cashi.collectionmanagement.domain.model.entities.ManagementDynamicField;
import com.cashi.collectionmanagement.domain.model.entities.PaymentDetail;
import com.cashi.collectionmanagement.domain.model.valueobjects.ContactResult;
import com.cashi.collectionmanagement.domain.model.valueobjects.ManagementId;
import com.cashi.collectionmanagement.domain.model.valueobjects.ManagementType;
import com.cashi.shared.domain.AggregateRoot;
import com.cashi.shared.domain.model.entities.Campaign;
import com.cashi.shared.domain.model.entities.Portfolio;
import com.cashi.shared.domain.model.entities.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "gestiones", indexes = {
    @Index(name = "idx_gest_inquilino", columnList = "id_inquilino"),
    @Index(name = "idx_gest_cartera", columnList = "id_cartera"),
    @Index(name = "idx_gest_campana", columnList = "id_campana"),
    @Index(name = "idx_gest_cliente", columnList = "id_cliente"),
    @Index(name = "idx_gest_asesor", columnList = "id_asesor"),
    @Index(name = "idx_gest_fecha", columnList = "fecha_gestion")
})
@Getter
@NoArgsConstructor
public class Management extends AggregateRoot {

    @Embedded
    private ManagementId managementId;

    // Multi-tenant fields (nullable for backward compatibility)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_inquilino")
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cartera")
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_campana")
    private Campaign campaign;

    @Column(name = "id_cliente")
    private String customerId;

    @Column(name = "id_asesor")
    private String advisorId;

    @Column(name = "id_campana_legacy")
    private String campaignId;

    @Column(name = "fecha_gestion")
    private LocalDateTime managementDate;

    // CLASIFICACIÓN: Categoría/grupo al que pertenece la tipificación
    @Column(name = "codigo_clasificacion", length = 50)
    private String classificationCode;

    @Column(name = "descripcion_clasificacion", length = 255)
    private String classificationDescription;

    // TIPIFICACIÓN: Código específico/hoja (último nivel en jerarquía)
    @Column(name = "codigo_tipificacion", length = 50)
    private String typificationCode;

    @Column(name = "descripcion_tipificacion", length = 255)
    private String typificationDescription;

    @Column(name = "tipificacion_requiere_pago")
    private Boolean typificationRequiresPayment;

    @Column(name = "tipificacion_requiere_cronograma")
    private Boolean typificationRequiresSchedule;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "id_detalle_llamada")
    private CallDetail callDetail;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "id_detalle_pago")
    private PaymentDetail paymentDetail;

    @Column(name = "observaciones", length = 2000)
    private String observations;

    // Campos dinámicos en formato JSON (más simple y flexible)
    @Column(name = "campos_dinamicos_json", columnDefinition = "JSON")
    private String dynamicFieldsJson;

    // Dynamic fields for multi-tenant support (EAV pattern - legacy)
    @OneToMany(mappedBy = "management", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ManagementDynamicField> dynamicFields = new ArrayList<>();

    // Normalized classifications (supports N levels)
    @OneToMany(mappedBy = "management", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<com.cashi.collectionmanagement.domain.model.entities.ManagementClassification> classifications = new ArrayList<>();

    // Legacy constructor (backward compatibility)
    public Management(String customerId, String advisorId, String campaignId) {
        this.managementId = ManagementId.generate();
        this.customerId = customerId;
        this.advisorId = advisorId;
        this.campaignId = campaignId;
        this.managementDate = LocalDateTime.now();
    }

    // Multi-tenant constructor
    public Management(Tenant tenant, Portfolio portfolio, Campaign campaign,
                     String customerId, String advisorId) {
        this.managementId = ManagementId.generate();
        this.tenant = tenant;
        this.portfolio = portfolio;
        this.campaign = campaign;
        this.customerId = customerId;
        this.advisorId = advisorId;
        this.managementDate = LocalDateTime.now();
    }

    // Multi-tenant constructor (tenant only)
    public Management(Tenant tenant, String customerId, String advisorId) {
        this.managementId = ManagementId.generate();
        this.tenant = tenant;
        this.customerId = customerId;
        this.advisorId = advisorId;
        this.managementDate = LocalDateTime.now();
    }

    // Dynamic field management
    public void addDynamicField(ManagementDynamicField dynamicField) {
        dynamicFields.add(dynamicField);
        dynamicField.setManagement(this);
    }

    public void removeDynamicField(ManagementDynamicField dynamicField) {
        dynamicFields.remove(dynamicField);
        dynamicField.setManagement(null);
    }

    public void clearDynamicFields() {
        dynamicFields.clear();
    }

    // Classification management (normalized)
    public void addClassification(com.cashi.collectionmanagement.domain.model.entities.ManagementClassification classification) {
        classifications.add(classification);
        classification.setManagement(this);
    }

    public void removeClassification(com.cashi.collectionmanagement.domain.model.entities.ManagementClassification classification) {
        classifications.remove(classification);
        classification.setManagement(null);
    }

    public void clearClassifications() {
        classifications.clear();
    }

    public void setCallDetail(CallDetail callDetail) {
        this.callDetail = callDetail;
    }

    public void setPaymentDetail(PaymentDetail paymentDetail) {
        this.paymentDetail = paymentDetail;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public void setDynamicFieldsJson(String dynamicFieldsJson) {
        this.dynamicFieldsJson = dynamicFieldsJson;
    }

    // New setters for Classification and Typification
    public void setClassification(String code, String description) {
        this.classificationCode = code;
        this.classificationDescription = description;
    }

    public void setTypification(String code, String description, Boolean requiresPayment, Boolean requiresSchedule) {
        this.typificationCode = code;
        this.typificationDescription = description;
        this.typificationRequiresPayment = requiresPayment;
        this.typificationRequiresSchedule = requiresSchedule;
    }

}
