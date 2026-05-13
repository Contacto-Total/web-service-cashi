package com.cashi.osiptelvalidation.interfaces.rest.controllers;

import com.cashi.osiptelvalidation.domain.model.commands.EnqueueOsiptelBatchCommand;
import com.cashi.osiptelvalidation.domain.model.queries.GetValidationByPhoneQuery;
import com.cashi.osiptelvalidation.domain.model.queries.GetValidationMetricsByPortfolioQuery;
import com.cashi.osiptelvalidation.domain.services.OsiptelValidationCommandService;
import com.cashi.osiptelvalidation.domain.services.OsiptelValidationQueryService;
import com.cashi.osiptelvalidation.infrastructure.config.OsiptelProperties;
import com.cashi.osiptelvalidation.interfaces.rest.resources.EnqueueBatchRequest;
import com.cashi.osiptelvalidation.interfaces.rest.resources.EnqueueBatchResource;
import com.cashi.osiptelvalidation.interfaces.rest.resources.ValidationStatusResource;
import com.cashi.osiptelvalidation.interfaces.rest.transform.OsiptelValidationResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Endpoints públicos del módulo Osiptel.
 *
 * Bloqueo legal: si profile=prod y legal-review.signed-off=false, todos los endpoints
 * retornan HTTP 423 (Locked) con un mensaje explicativo. Esto cierra la puerta a
 * uso accidental en producción antes de la aprobación de Legal.
 */
@RestController
@RequestMapping("/api/v1/osiptel")
@Tag(name = "Osiptel", description = "Validación de titularidad telefónica vía Osiptel (Fase 1 standalone)")
public class OsiptelValidationController {

    private static final Logger log = LoggerFactory.getLogger(OsiptelValidationController.class);

    private final OsiptelValidationCommandService commandService;
    private final OsiptelValidationQueryService queryService;
    private final OsiptelValidationResourceAssembler assembler;
    private final OsiptelProperties properties;
    private final Environment environment;

    public OsiptelValidationController(OsiptelValidationCommandService commandService,
                                       OsiptelValidationQueryService queryService,
                                       OsiptelValidationResourceAssembler assembler,
                                       OsiptelProperties properties,
                                       Environment environment) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.assembler = assembler;
        this.properties = properties;
        this.environment = environment;
    }

    @PostMapping("/batches")
    @Operation(summary = "Encolar lote de números para validación Osiptel")
    public ResponseEntity<?> enqueueBatch(@Valid @RequestBody EnqueueBatchRequest request) {
        ResponseEntity<?> blocked = blockIfNotSignedOff();
        if (blocked != null) return blocked;

        List<EnqueueOsiptelBatchCommand.PhoneEntry> entries = request.phones().stream()
                .map(p -> new EnqueueOsiptelBatchCommand.PhoneEntry(
                        p.phone(), p.dni(), p.subPortfolioId(), p.contactMethodId(), p.tenantId()))
                .toList();

        var result = commandService.enqueueBatch(new EnqueueOsiptelBatchCommand(entries));
        return ResponseEntity.ok(new EnqueueBatchResource(
                result.enqueued(), result.skipped(), result.batchId()));
    }

    @GetMapping("/validations/{phone}")
    @Operation(summary = "Obtener última validación conocida para un número")
    public ResponseEntity<?> getValidation(@PathVariable String phone) {
        ResponseEntity<?> blocked = blockIfNotSignedOff();
        if (blocked != null) return blocked;

        return queryService.findLatestByPhone(new GetValidationByPhoneQuery(phone))
                .map(assembler::toResourceFromEntity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(emptyResource(phone)));
    }

    @GetMapping("/metrics")
    @Operation(summary = "Métricas agregadas por subcartera y rango")
    public ResponseEntity<?> getMetrics(@RequestParam(required = false) Long subPortfolioId,
                                        @RequestParam(required = false) String from,
                                        @RequestParam(required = false) String to) {
        ResponseEntity<?> blocked = blockIfNotSignedOff();
        if (blocked != null) return blocked;

        LocalDateTime fromTs = from == null ? null : LocalDateTime.parse(from);
        LocalDateTime toTs = to == null ? null : LocalDateTime.parse(to);
        var metrics = queryService.getMetrics(
                new GetValidationMetricsByPortfolioQuery(subPortfolioId, fromTs, toTs));
        return ResponseEntity.ok(metrics);
    }

    /**
     * Devuelve 423 (Locked) si el environment es prod y Legal no ha firmado.
     * En perfiles dev/test el bloqueo se omite para permitir desarrollo y E2E.
     */
    private ResponseEntity<?> blockIfNotSignedOff() {
        if (properties.isLegalReviewSignedOff()) {
            return null;
        }
        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (!isProd) {
            return null;
        }
        log.warn("Osiptel endpoint bloqueado: legal-review.signed-off=false en perfil prod");
        return ResponseEntity.status(HttpStatus.LOCKED).body(Map.of(
                "error", "OSIPTEL_LEGAL_REVIEW_PENDING",
                "message", "El uso del módulo Osiptel requiere aprobación de Legal. " +
                           "Active cashi.osiptel.legal-review.signed-off=true tras el sign-off."
        ));
    }

    private ValidationStatusResource emptyResource(String phone) {
        return new ValidationStatusResource(phone, "NOT_CHECKED", null, null, null, null, 0);
    }
}
