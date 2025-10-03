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
@Table(name = "managements", indexes = {
    @Index(name = "idx_mgmt_tenant", columnList = "tenant_id"),
    @Index(name = "idx_mgmt_portfolio", columnList = "portfolio_id"),
    @Index(name = "idx_mgmt_campaign", columnList = "campaign_id"),
    @Index(name = "idx_mgmt_customer", columnList = "customer_id"),
    @Index(name = "idx_mgmt_advisor", columnList = "advisor_id"),
    @Index(name = "idx_mgmt_date", columnList = "management_date")
})
@Getter
@NoArgsConstructor
public class Management extends AggregateRoot {

    @Embedded
    private ManagementId managementId;

    // Multi-tenant fields (nullable for backward compatibility)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    private String customerId;

    private String advisorId;

    @Deprecated(forRemoval = true)
    @Column(name = "legacy_campaign_id")
    private String campaignId;

    private LocalDateTime managementDate;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "code", column = @Column(name = "contact_result_code")),
        @AttributeOverride(name = "description", column = @Column(name = "contact_result_description"))
    })
    private ContactResult contactResult;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "code", column = @Column(name = "management_type_code")),
        @AttributeOverride(name = "description", column = @Column(name = "management_type_description")),
        @AttributeOverride(name = "requiresPayment", column = @Column(name = "management_type_requires_payment")),
        @AttributeOverride(name = "requiresSchedule", column = @Column(name = "management_type_requires_schedule"))
    })
    private ManagementType managementType;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "call_detail_id")
    private CallDetail callDetail;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "payment_detail_id")
    private PaymentDetail paymentDetail;

    @Column(length = 2000)
    private String observations;

    // Dynamic fields for multi-tenant support (EAV pattern)
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

    public void setContactResult(ContactResult contactResult) {
        this.contactResult = contactResult;
    }

    public void setManagementType(ManagementType managementType) {
        this.managementType = managementType;
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

    public void updateManagement(ContactResult contactResult, ManagementType managementType, String observations) {
        this.contactResult = contactResult;
        this.managementType = managementType;
        this.observations = observations;
    }
}
