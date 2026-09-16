package com.etra.cti.organization.dto;

import com.etra.cti.organization.Organization;
import com.etra.cti.user.UserRole;

import java.util.UUID;

public record OrganizationDto(UUID id, String name, String slug, UserRole myRole) {
    public static OrganizationDto from(Organization o, UserRole role) {
        return new OrganizationDto(o.getId(), o.getName(), o.getSlug(), role);
    }
}
