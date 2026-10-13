package com.etra.cti.investigation.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignInvestigationRequest(@NotNull UUID assignedTo) {}
