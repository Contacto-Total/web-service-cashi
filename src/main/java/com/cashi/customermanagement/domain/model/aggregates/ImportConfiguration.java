package com.cashi.customermanagement.domain.model.aggregates;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "configuraciones_importacion")
@Getter
@Setter
public class ImportConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_configuracion")
    private Long id;

    @Column(name = "directorio_monitoreado", nullable = false, length = 500)
    private String watchDirectory;

    @Column(name = "patron_archivo", nullable = false, length = 100)
    private String filePattern;

    @Column(name = "id_subcartera", nullable = false)
    private Integer subPortfolioId;

    @Column(name = "hora_programada", length = 8, nullable = false)
    private String scheduledTime; // Formato: "HH:mm:ss" (ej: "02:00:00")

    @Column(name = "activo", nullable = false)
    private Boolean active;

    @Column(name = "directorio_procesados", length = 500)
    private String processedDirectory;

    @Column(name = "directorio_errores", length = 500)
    private String errorDirectory;

    @Column(name = "mover_despues_procesar", nullable = false)
    private Boolean moveAfterProcess;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime updatedAt;

    @Column(name = "ultima_revision")
    private LocalDateTime lastCheckAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (active == null) {
            active = false;
        }
        if (moveAfterProcess == null) {
            moveAfterProcess = true;
        }
        if (scheduledTime == null) {
            scheduledTime = "02:00:00"; // Default: 2:00 AM
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public ImportConfiguration() {
    }

    public ImportConfiguration(String watchDirectory, String filePattern, String scheduledTime) {
        this.watchDirectory = watchDirectory;
        this.filePattern = filePattern;
        this.scheduledTime = scheduledTime;
        this.active = true;
        this.moveAfterProcess = true;
    }
}
