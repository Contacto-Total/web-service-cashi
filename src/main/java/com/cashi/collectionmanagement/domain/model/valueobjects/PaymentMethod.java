package com.cashi.collectionmanagement.domain.model.valueobjects;

import com.cashi.shared.domain.ValueObject;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethod implements ValueObject {

    private String type;
    private String details;
}
