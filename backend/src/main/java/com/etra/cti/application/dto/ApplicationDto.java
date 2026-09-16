package com.etra.cti.application.dto;

import com.etra.cti.application.*;

import java.util.UUID;

public record ApplicationDto(
    UUID id, UUID projectId, String name,
    ApplicationType type, Environment environment,
    Criticality criticality, String baseUrl, UUID ownerId
) {
    public static ApplicationDto from(Application a) {
        return new ApplicationDto(a.getId(), a.getProjectId(), a.getName(),
            a.getType(), a.getEnvironment(), a.getCriticality(), a.getBaseUrl(), a.getOwnerId());
    }
}
