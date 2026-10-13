package com.etra.cti.event.dto;

import com.etra.cti.event.EventSource;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record IngestEventRequest(
    @NotNull UUID applicationId,
    @NotNull EventSource source,
    @NotNull Instant timestamp,
    String sourceIp,
    String method,
    String path,
    Integer statusCode,
    String userId,
    Map<String, Object> metadata
) {}
