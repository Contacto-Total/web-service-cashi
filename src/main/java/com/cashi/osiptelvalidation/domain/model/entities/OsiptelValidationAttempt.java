package com.cashi.osiptelvalidation.domain.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Historial detallado por intento. Soporta debugging y métricas operativas
 * (latencias, ratios de captcha, tasa de errores por worker).
 *
 * No es AggregateRoot - vive bajo el ciclo de vida de OsiptelValidation
 * (ON DELETE CASCADE en la FK).
 */
@Entity
@Table(name = "osiptel_validation_attempt")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
public class OsiptelValidationAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "validation_id", nullable = false)
    private Long validationId;

    @Column(name = "attempt_no", nullable = false)
    private Short attemptNo;

    @Column(name = "http_status")
    private Short httpStatus;

    @Column(name = "captcha_attempts", nullable = false)
    private Short captchaAttempts;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "result_status", length = 20, nullable = false)
    private String resultStatus;

    @Column(name = "error_detail", length = 255)
    private String errorDetail;

    @Column(name = "worker_id", length = 40)
    private String workerId;

    @CreatedDate
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime createdAt;

    public OsiptelValidationAttempt(Long validationId, Short attemptNo,
                                    Short httpStatus, Short captchaAttempts,
                                    Integer latencyMs, String resultStatus,
                                    String errorDetail, String workerId) {
        this.validationId = validationId;
        this.attemptNo = attemptNo;
        this.httpStatus = httpStatus;
        this.captchaAttempts = captchaAttempts != null ? captchaAttempts : 0;
        this.latencyMs = latencyMs;
        this.resultStatus = resultStatus;
        this.errorDetail = errorDetail;
        this.workerId = workerId;
        this.createdAt = LocalDateTime.now();
    }
}
