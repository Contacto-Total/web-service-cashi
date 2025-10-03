package com.cashi.customermanagement.domain.model.valueobjects;

import com.cashi.shared.domain.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
public class DocumentNumber implements ValueObject {

    @Column(name = "document_type", nullable = false, length = 10)
    private String type;

    @Column(name = "document_number", nullable = false, length = 20)
    private String number;

    public DocumentNumber(String type, String number) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Document type cannot be empty");
        }
        if (number == null || number.trim().isEmpty()) {
            throw new IllegalArgumentException("Document number cannot be empty");
        }
        this.type = type;
        this.number = number;
    }
}
