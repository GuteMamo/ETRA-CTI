package com.etra.cti.threatintel.dto;

import com.etra.cti.detection.Severity;
import com.etra.cti.threatintel.Ioc;
import com.etra.cti.threatintel.IocType;

import java.time.Instant;
import java.util.UUID;

public record IocDto(
    UUID id, IocType type, String value, String description,
    String source, Severity severity, Boolean active,
    Instant expiresAt, Instant createdAt
) {
    public static IocDto from(Ioc i) {
        return new IocDto(i.getId(), i.getType(), i.getValue(), i.getDescription(),
            i.getSource(), i.getSeverity(), i.getActive(),
            i.getExpiresAt(), i.getCreatedAt());
    }
}
