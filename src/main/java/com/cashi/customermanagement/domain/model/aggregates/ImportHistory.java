package com.cashi.customermanagement.domain.model.aggregates;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "historial_importaciones")
@Getter
@Setter
public class ImportHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Long id;

    @Column(name = "id_subcartera", nullable = false)
    private Long subPortfolioId;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String fileName;

    @Column(name = "ruta_archivo", nullable = false, length = 500)
    private String filePath;

    @Column(name = "fecha_procesamiento", nullable = false)
    private LocalDateTime processedAt;

    @Column(name = "estado", nullable = false, length = 20)
    private String status; // SUCCESS, ERROR

    @Column(name = "registros_procesados")
    private Integer recordsProcessed;

    @Column(name = "mensaje_error", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (processedAt == null) {
            processedAt = LocalDateTime.now();
        }
    }

    public ImportHistory() {
    }

    public ImportHistory(Long subPortfolioId, String fileName, String filePath, String status,
                        Integer recordsProcessed, String errorMessage) {
        this.subPortfolioId = subPortfolioId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.status = status;
        this.recordsProcessed = recordsProcessed;
        this.errorMessage = errorMessage;
        this.processedAt = LocalDateTime.now();
    }
}
