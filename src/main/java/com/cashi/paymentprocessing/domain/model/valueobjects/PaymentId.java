package com.cashi.paymentprocessing.domain.model.valueobjects;

import com.cashi.shared.domain.ValueObject;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor
public class PaymentId implements ValueObject {

    private String paymentId;

    public PaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public static PaymentId generate() {
        return new PaymentId(UUID.randomUUID().toString());
    }
}
