package com.cashi.paymentprocessing.domain.model.valueobjects;

import com.cashi.shared.domain.ValueObject;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatus implements ValueObject {

    private String status;
    private String description;

    public boolean isPending() {
        return "PENDING".equals(status);
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    public boolean isCancelled() {
        return "CANCELLED".equals(status);
    }

    public static PaymentStatus pending() {
        return new PaymentStatus("PENDING", "Pago pendiente");
    }

    public static PaymentStatus completed() {
        return new PaymentStatus("COMPLETED", "Pago completado");
    }

    public static PaymentStatus cancelled() {
        return new PaymentStatus("CANCELLED", "Pago cancelado");
    }
}
