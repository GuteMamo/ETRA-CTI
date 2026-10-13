package com.etra.cti.remediation.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StartRemediationRequest(@NotNull UUID developerId) {}
