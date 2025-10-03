package com.cashi.collectionmanagement.domain.model.valueobjects;

import com.cashi.shared.domain.ValueObject;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor
public class ManagementId implements ValueObject {

    private String managementId;

    public ManagementId(String managementId) {
        this.managementId = managementId;
    }

    public static ManagementId generate() {
        return new ManagementId(UUID.randomUUID().toString());
    }
}
