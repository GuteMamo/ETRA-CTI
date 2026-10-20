package com.etra.cti.alert;

import jakarta.validation.constraints.NotNull;

public record TriageAlertRequest(
    @NotNull AlertStatus status,
    String notes
) {}
