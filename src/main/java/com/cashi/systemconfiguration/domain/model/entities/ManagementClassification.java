package com.cashi.systemconfiguration.domain.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ManagementClassification Entity
 * Representa los tipos de gestión (ACP, PGR, CNV, etc.)
 * @deprecated Use ClassificationCatalog instead
 */
@Deprecated(forRemoval = true)
@Entity(name = "LegacyManagementClassification")
@Table(name = "legacy_management_classifications")
@Getter
@NoArgsConstructor
public class ManagementClassification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 10)
    private String code;

    @Column(nullable = false)
    private String label;

    @Column(name = "requires_payment")
    private Boolean requiresPayment;

    @Column(name = "requires_schedule")
    private Boolean requiresSchedule;

    @Column(name = "requires_follow_up")
    private Boolean requiresFollowUp;

    public ManagementClassification(String code, String label, Boolean requiresPayment,
                                   Boolean requiresSchedule, Boolean requiresFollowUp) {
        this.code = code;
        this.label = label;
        this.requiresPayment = requiresPayment;
        this.requiresSchedule = requiresSchedule;
        this.requiresFollowUp = requiresFollowUp;
    }
}
