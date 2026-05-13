package com.cashi.osiptelvalidation.interfaces.rest.transform;

import com.cashi.osiptelvalidation.domain.model.aggregates.OsiptelValidation;
import com.cashi.osiptelvalidation.interfaces.rest.resources.ValidationStatusResource;
import org.springframework.stereotype.Component;

@Component
public class OsiptelValidationResourceAssembler {

    public ValidationStatusResource toResourceFromEntity(OsiptelValidation v) {
        return new ValidationStatusResource(
                v.getPhone(),
                v.getStatus().name(),
                v.getDniMatch(),
                v.getOperator() == null ? null : v.getOperator().name(),
                v.getFinishedAt(),
                v.getCooldownUntil(),
                v.getAttempts() == null ? 0 : v.getAttempts().intValue()
        );
    }
}
