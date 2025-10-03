package com.cashi.collectionmanagement.domain.model.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "call_details")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CallDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String phoneNumber;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

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
