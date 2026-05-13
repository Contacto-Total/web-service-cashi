package com.cashi.osiptelvalidation.application.services;

import com.cashi.osiptelvalidation.domain.services.OsiptelValidationCommandService;
import com.cashi.osiptelvalidation.infrastructure.config.OsiptelProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Housekeeping de la cola Osiptel.
 *
 * Post-pivot a Electron app: ya NO empuja jobs al worker. El Electron app
 * (cashi-desktop-scraper, repo aparte) polea POST /api/v1/osiptel/jobs/claim
 * para reclamar trabajo desde una IP residencial peruana (necesario porque
 * el WAF de Osiptel bloquea IPs de cloud providers).
 *
 * Esta clase solo conserva el reclamo de filas IN_PROGRESS huérfanas
 * (workers que murieron a mitad del trabajo y nunca llamaron al callback).
 */
@Service
public class OsiptelDispatcherService {

    private final OsiptelValidationCommandService commandService;
    private final OsiptelAuditService audit;
    private final OsiptelProperties properties;

    public OsiptelDispatcherService(OsiptelValidationCommandService commandService,
                                    OsiptelAuditService audit,
                                    OsiptelProperties properties) {
        this.commandService = commandService;
        this.audit = audit;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${cashi.osiptel.dispatcher-interval-ms:30000}")
    public void housekeepingCycle() {
        if (!properties.isDispatcherEnabled() || !properties.isLegalReviewSignedOff()) {
            return;
        }
        int reclaimed = commandService.reclaimStuckInProgress(properties.getStuckThresholdMinutes());
        if (reclaimed > 0) {
            audit.recordReclaim(reclaimed);
        }
    }
}
