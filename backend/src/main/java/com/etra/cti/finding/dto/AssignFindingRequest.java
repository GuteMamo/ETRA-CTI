package com.etra.cti.finding.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record AssignFindingRequest(
    @NotNull UUID assignedTo,
    Instant dueDate
) {}
