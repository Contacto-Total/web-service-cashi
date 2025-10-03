package com.cashi.collectionmanagement.domain.model.valueobjects;

import com.cashi.shared.domain.ValueObject;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContactResult implements ValueObject {

    private String code;
    private String description;
}
