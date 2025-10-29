package com.cashi.shared.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Base class for all Aggregate Roots in the system
 * Provides common functionality like auditing
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class AggregateRoot implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "fecha_actualizacion")
    private LocalDateTime updatedAt;

    protected AggregateRoot() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    protected void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}
