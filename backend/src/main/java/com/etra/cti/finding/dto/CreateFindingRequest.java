package com.etra.cti.finding.dto;

import com.etra.cti.detection.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateFindingRequest(
    @NotNull UUID applicationId,
    UUID detectionId,
    @NotBlank String title,
    String description,
    String endpoint,
    String parameter,
    @NotNull Severity severity,
    BigDecimal confidence,
    String source
) {}
