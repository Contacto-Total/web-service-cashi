package com.cashi.systemconfiguration.application.internal.queryservices;

import com.cashi.shared.domain.model.entities.Portfolio;
import com.cashi.shared.domain.model.entities.Tenant;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.PortfolioRepository;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.TenantRepository;
import com.cashi.systemconfiguration.domain.model.aggregates.Campaign;
import com.cashi.systemconfiguration.domain.model.entities.ContactClassification;
import com.cashi.systemconfiguration.domain.model.entities.ManagementClassification;
import com.cashi.systemconfiguration.infrastructure.persistence.jpa.repositories.LegacyCampaignRepository;
import com.cashi.systemconfiguration.infrastructure.persistence.jpa.repositories.LegacyContactClassificationRepository;
import com.cashi.systemconfiguration.infrastructure.persistence.jpa.repositories.LegacyManagementClassificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SystemConfigQueryServiceImpl {

    private final LegacyCampaignRepository campaignRepository;
    private final LegacyContactClassificationRepository contactClassificationRepository;
    private final LegacyManagementClassificationRepository managementClassificationRepository;
    private final PortfolioRepository portfolioRepository;
    private final TenantRepository tenantRepository;

    public SystemConfigQueryServiceImpl(
            LegacyCampaignRepository campaignRepository,
            LegacyContactClassificationRepository contactClassificationRepository,
            LegacyManagementClassificationRepository managementClassificationRepository,
            PortfolioRepository portfolioRepository,
            TenantRepository tenantRepository) {
        this.campaignRepository = campaignRepository;
        this.contactClassificationRepository = contactClassificationRepository;
        this.managementClassificationRepository = managementClassificationRepository;
        this.portfolioRepository = portfolioRepository;
        this.tenantRepository = tenantRepository;
    }

    public List<Campaign> getAllCampaigns() {
        return campaignRepository.findAll();
    }

    public List<Campaign> getActiveCampaigns() {
        return campaignRepository.findByIsActive(true);
    }

    public Optional<Campaign> getCampaignById(String campaignId) {
        return campaignRepository.findByCampaignId(campaignId);
    }

    public List<ContactClassification> getAllContactClassifications() {
        return contactClassificationRepository.findAll();
    }

    public List<ManagementClassification> getAllManagementClassifications() {
        return managementClassificationRepository.findAll();
    }

    public List<ManagementClassification> getManagementClassificationsByPaymentRequirement(Boolean requiresPayment) {
        return managementClassificationRepository.findByRequiresPayment(requiresPayment);
    }

    public List<ManagementClassification> getManagementClassificationsByScheduleRequirement(Boolean requiresSchedule) {
        return managementClassificationRepository.findByRequiresSchedule(requiresSchedule);
    }

    public List<Portfolio> getPortfoliosByTenantId(Long tenantId) {
        Optional<Tenant> tenantOpt = tenantRepository.findById(tenantId);
        if (tenantOpt.isEmpty()) {
            return List.of();
        }
        return portfolioRepository.findByTenantOrderedByHierarchy(tenantOpt.get());
    }

    public List<Tenant> getAllActiveTenants() {
        return tenantRepository.findAll().stream()
            .filter(Tenant::getIsActive)
            .toList();
    }
}
