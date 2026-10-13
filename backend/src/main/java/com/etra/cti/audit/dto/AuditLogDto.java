package com.etra.cti.audit.dto;

import com.etra.cti.audit.AuditLog;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLogDto(
    UUID id, UUID userId, String action,
    String resourceType, String resourceId,
    Map<String, Object> oldValue, Map<String, Object> newValue,
    String ipAddress, Instant createdAt
) {
    public static AuditLogDto from(AuditLog a) {
        return new AuditLogDto(a.getId(), a.getUserId(), a.getAction(),
            a.getResourceType(), a.getResourceId(),
            a.getOldValue(), a.getNewValue(),
            a.getIpAddress(), a.getCreatedAt());
    }
}
