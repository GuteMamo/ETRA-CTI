package com.etra.cti.event.dto;

import com.etra.cti.event.EventSource;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IngestEventRequest(
    @JsonAlias({"application_id", "appId", "app_id"})
    UUID applicationId,

    @JsonAlias({"sourceType", "source_type", "event_source", "eventSource"})
    EventSource source,

    @JsonAlias({"eventTime", "event_time", "time", "date"})
    Instant timestamp,

    @JsonAlias({"source_ip", "ip", "client_ip", "clientIp", "remote_ip", "remoteIp"})
    String sourceIp,

    @JsonAlias({"httpMethod", "http_method"})
    String method,

    @JsonAlias({"endpoint", "uri", "url", "route"})
    String path,

    @JsonAlias({"status_code", "status", "code"})
    Integer statusCode,

    @JsonAlias({"actor", "user_id", "userIdentifier", "user_identifier", "username"})
    String userId,

    @JsonAlias({"meta", "data", "payload", "details", "headers"})
    Map<String, Object> metadata
) {}
