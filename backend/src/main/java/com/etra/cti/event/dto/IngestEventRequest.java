package com.etra.cti.event.dto;

import com.etra.cti.event.EventSource;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IngestEventRequest(
    @NotNull
    UUID applicationId,

    @NotNull
    @JsonAlias({"sourceType", "source_type"})
    EventSource source,

    @NotNull
    @JsonAlias({"eventTime", "event_time", "time"})
    Instant timestamp,

    @JsonAlias({"source_ip", "ip"})
    String sourceIp,

    @JsonAlias({"httpMethod", "http_method"})
    String method,

    @JsonAlias({"endpoint", "uri", "url"})
    String path,

    @JsonAlias({"status_code", "status"})
    Integer statusCode,

    @JsonAlias({"actor", "user_id", "userIdentifier"})
    String userId,

    @JsonAlias({"meta", "data"})
    Map<String, Object> metadata
) {}
