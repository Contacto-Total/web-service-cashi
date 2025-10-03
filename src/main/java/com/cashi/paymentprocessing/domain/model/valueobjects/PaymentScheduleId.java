package com.cashi.paymentprocessing.domain.model.valueobjects;

import com.cashi.shared.domain.ValueObject;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor
public class PaymentScheduleId implements ValueObject {

    private String scheduleId;

    public PaymentScheduleId(String scheduleId) {
        this.scheduleId = scheduleId;
    }

    public static PaymentScheduleId generate() {
        return new PaymentScheduleId(UUID.randomUUID().toString());
    }
}
