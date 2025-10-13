package com.cashi.systemconfiguration.interfaces.rest.transform;

import com.cashi.systemconfiguration.domain.model.entities.ClassificationCatalog;
import com.cashi.systemconfiguration.domain.model.entities.TenantClassificationConfig;
import com.cashi.systemconfiguration.interfaces.rest.resources.ClassificationCatalogResource;
import com.cashi.systemconfiguration.interfaces.rest.resources.TenantClassificationConfigResource;

public class ClassificationResourceAssembler {

    public static ClassificationCatalogResource toResourceFromEntity(ClassificationCatalog entity) {
        return new ClassificationCatalogResource(
            entity.getId(),
            entity.getCode(),
            entity.getName(),
            entity.getClassificationType().name(),
            entity.getParentClassification() != null ? entity.getParentClassification().getId() : null,
            entity.getHierarchyLevel(),
            entity.getHierarchyPath(),
            entity.getDescription(),
            entity.getDisplayOrder(),
            entity.getIconName(),
            entity.getColorHex(),
            entity.getMetadataSchema(),
            entity.getIsSystem(),
            entity.getIsActive()
        );
    }

    public static TenantClassificationConfigResource toResourceFromConfig(TenantClassificationConfig config) {
        return new TenantClassificationConfigResource(
            config.getId(),
            config.getTenant().getId(),
            config.getPortfolio() != null ? config.getPortfolio().getId() : null,
            toResourceFromEntity(config.getClassification()),
            config.getIsEnabled(),
            config.getIsRequired(),
            config.getCustomName(),
            config.getCustomOrder(),
            config.getCustomIcon(),
            config.getCustomColor(),
            config.getRequiresComment(),
            config.getMinCommentLength(),
            config.getRequiresAttachment(),
            config.getRequiresFollowupDate(),
            config.getEffectiveName(),
            config.getEffectiveOrder(),
            config.getEffectiveIcon(),
            config.getEffectiveColor()
        );
    }
}
