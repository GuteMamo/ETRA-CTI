package com.etra.cti.finding.dto;

import com.etra.cti.detection.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OverridePriorityRequest(
    @NotNull Severity severity,
    @NotBlank String reason
) {}
