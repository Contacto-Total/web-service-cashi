package com.cashi.osiptelvalidation.domain.model.entities;

import com.cashi.osiptelvalidation.domain.model.valueobjects.OperatorCode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Resultado del matching entre un teléfono concreto del cliente y las líneas
 * devueltas por el portal Osiptel para su DNI.
 *
 * Se genera por cada teléfono móvil PE del cliente tras una validación exitosa.
 * El match es por prefijo de 5 dígitos (el portal sólo expone los primeros 5,
 * los últimos 4 están enmascarados). Es probabilístico: ~1/10000 falso positivo
 * por número, aceptable para cobranza.
 */
@Entity
@Table(name = "osiptel_phone_match")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
public class OsiptelPhoneMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "validation_id", nullable = false)
    private Long validationId;

    @Column(name = "phone", length = 15, nullable = false)
    private String phone;

    @Column(name = "phone_prefix", length = 5, nullable = false)
    private String phonePrefix;

    @Column(name = "dni_match", nullable = false)
    private Boolean dniMatch;

    @Enumerated(EnumType.STRING)
    @Column(name = "matched_operator", length = 20)
    private OperatorCode matchedOperator;

    @Column(name = "matched_modality", length = 20)
    private String matchedModality;

    @CreatedDate
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime createdAt;

    public OsiptelPhoneMatch(Long validationId, String phone, String phonePrefix,
                             Boolean dniMatch, OperatorCode matchedOperator, String matchedModality) {
        this.validationId = validationId;
        this.phone = phone;
        this.phonePrefix = phonePrefix;
        this.dniMatch = dniMatch;
        this.matchedOperator = matchedOperator;
        this.matchedModality = matchedModality;
        this.createdAt = LocalDateTime.now();
    }
}
