package com.cashi.collectionmanagement.domain.model.entities;

import com.cashi.collectionmanagement.domain.model.aggregates.Management;
import com.cashi.systemconfiguration.domain.model.entities.ClassificationCatalog;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * ManagementClassification - Vincula gestiones con clasificaciones multinivel
 * Soporta jerarquías de N niveles (nivel 1, 2, 3, ... N)
 */
@Entity
@Table(name = "management_classifications", indexes = {
    @Index(name = "idx_mgmt_class_management", columnList = "management_id"),
    @Index(name = "idx_mgmt_class_classification", columnList = "classification_id"),
    @Index(name = "idx_mgmt_class_level", columnList = "management_id, hierarchy_level"),
    @Index(name = "idx_mgmt_class_unique", columnList = "management_id, hierarchy_level", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class ManagementClassification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "management_id", nullable = false)
    private Management management;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classification_id", nullable = false)
    private ClassificationCatalog classification;

    @Column(name = "hierarchy_level", nullable = false)
    private Integer hierarchyLevel;

    @CreatedDate
    @Column(name = "selected_at", nullable = false, updatable = false)
    private LocalDateTime selectedAt;

    public ManagementClassification(Management management, ClassificationCatalog classification,
                                   Integer hierarchyLevel) {
        this.management = management;
        this.classification = classification;
        this.hierarchyLevel = hierarchyLevel;
    }
}
