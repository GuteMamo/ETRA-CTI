package com.etra.cti.event.dto;

import com.etra.cti.event.EventSource;
import com.etra.cti.event.SecurityEvent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record EventDto(
    UUID id, UUID applicationId, EventSource source, Instant eventTime,
    String sourceIp, String method, String path, Integer statusCode,
    String userIdentifier, Map<String, Object> normalized
) {
    public static EventDto from(SecurityEvent e) {
        return new EventDto(e.getId(), e.getApplicationId(), e.getSource(), e.getEventTime(),
            e.getSourceIp(), e.getMethod(), e.getPath(), e.getStatusCode(),
            e.getUserIdentifier(), e.getNormalized());
    }
}
