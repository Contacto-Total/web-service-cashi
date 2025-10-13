package com.cashi.collectionmanagement.domain.model.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "detalles_llamada")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CallDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_telefono")
    private String phoneNumber;

    @Column(name = "hora_inicio")
    private LocalDateTime startTime;

    @Column(name = "hora_fin")
    private LocalDateTime endTime;

    @Column(name = "duracion_segundos")
    private Integer durationSeconds;

    public CallDetail(String phoneNumber, LocalDateTime startTime) {
        this.phoneNumber = phoneNumber;
        this.startTime = startTime;
    }

    public void endCall(LocalDateTime endTime) {
        this.endTime = endTime;
        if (startTime != null) {
            this.durationSeconds = (int) java.time.Duration.between(startTime, endTime).getSeconds();
        }
    }
}
