package com.etra.cti.investigation.dto;

import com.etra.cti.investigation.InvestigationPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateInvestigationRequest(
    UUID campaignId,
    @NotNull UUID applicationId,
    @NotBlank String title,
    String description,
    InvestigationPriority priority
) {}
