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
public class ManagementType implements ValueObject {

    private String code;
    private String description;
    private Boolean requiresPayment;
    private Boolean requiresSchedule;
}
