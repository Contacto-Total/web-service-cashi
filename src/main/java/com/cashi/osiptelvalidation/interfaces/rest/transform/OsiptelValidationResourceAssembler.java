package com.cashi.osiptelvalidation.interfaces.rest.transform;

import com.cashi.osiptelvalidation.domain.services.OsiptelValidationQueryService.PhoneValidationView;
import com.cashi.osiptelvalidation.interfaces.rest.resources.ValidationStatusResource;
import org.springframework.stereotype.Component;

@Component
public class OsiptelValidationResourceAssembler {

    public ValidationStatusResource toResource(PhoneValidationView v) {
        return new ValidationStatusResource(
                v.phone(),
                v.status(),
                v.dniMatch(),
                v.operator() == null ? null : v.operator().name(),
                v.modality(),
                v.checkedAt(),
                v.cooldownUntil(),
                v.attempts() == null ? 0 : v.attempts()
        );
    }
}
