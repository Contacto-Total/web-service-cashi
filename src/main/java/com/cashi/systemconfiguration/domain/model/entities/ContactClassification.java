package com.cashi.systemconfiguration.domain.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ContactClassification Entity
 * Representa las tipificaciones de contacto (CPC, CTT, NCL, etc.)
 * @deprecated Use ClassificationCatalog instead
 */
@Deprecated(forRemoval = true)
@Entity(name = "LegacyContactClassification")
@Table(name = "legacy_contact_classifications")
@Getter
@NoArgsConstructor
public class ContactClassification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 10)
    private String code;

    @Column(nullable = false)
    private String label;

    @Column(name = "is_successful")
    private Boolean isSuccessful;

    public ContactClassification(String code, String label, Boolean isSuccessful) {
        this.code = code;
        this.label = label;
        this.isSuccessful = isSuccessful;
    }
}
