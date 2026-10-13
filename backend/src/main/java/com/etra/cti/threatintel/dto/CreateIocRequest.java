package com.etra.cti.threatintel.dto;

import com.etra.cti.detection.Severity;
import com.etra.cti.threatintel.IocType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateIocRequest(
    @NotNull IocType type,
    @NotBlank String value,
    String description,
    String source,
    Severity severity,
    Instant expiresAt
) {}
