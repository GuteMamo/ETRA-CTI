package com.etra.cti.remediation.dto;

import com.etra.cti.remediation.RetestResult;
import jakarta.validation.constraints.NotNull;

public record PerformRetestRequest(
    @NotNull RetestResult result,
    String notes
) {}
