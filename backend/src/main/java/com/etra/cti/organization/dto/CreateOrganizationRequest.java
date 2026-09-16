package com.etra.cti.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(
    @NotBlank @Size(max = 200) String name,
    @NotBlank @Size(max = 100) String slug
) {}
