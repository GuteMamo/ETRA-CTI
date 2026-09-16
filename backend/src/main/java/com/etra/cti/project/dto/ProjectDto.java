package com.etra.cti.project.dto;

import com.etra.cti.project.Project;

import java.util.UUID;

public record ProjectDto(UUID id, UUID organizationId, String name, String description) {
    public static ProjectDto from(Project p) {
        return new ProjectDto(p.getId(), p.getOrganizationId(), p.getName(), p.getDescription());
    }
}
