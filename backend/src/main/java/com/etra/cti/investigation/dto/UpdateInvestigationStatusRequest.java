package com.etra.cti.investigation.dto;

import com.etra.cti.investigation.InvestigationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateInvestigationStatusRequest(@NotNull InvestigationStatus status) {}
