package com.etra.cti.application.apikey.dto;

import com.etra.cti.application.apikey.ApiKeyStatus;
import com.etra.cti.application.apikey.ApplicationApiKey;

import java.time.Instant;
import java.util.UUID;

public record ApiKeyDto(
    UUID id,
    UUID applicationId,
    String name,
    String keyPrefix,
    ApiKeyStatus status,
    Instant createdAt,
    Instant expiresAt,
    Instant revokedAt
) {
    public static ApiKeyDto from(ApplicationApiKey k) {
        return new ApiKeyDto(
            k.getId(),
            k.getApplicationId(),
            k.getName(),
            k.getKeyPrefix(),
            k.getStatus(),
            k.getCreatedAt(),
            k.getExpiresAt(),
            k.getRevokedAt()
        );
    }
}
