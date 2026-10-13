package com.etra.cti.finding.dto;

import com.etra.cti.finding.FindingStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull FindingStatus status) {}
