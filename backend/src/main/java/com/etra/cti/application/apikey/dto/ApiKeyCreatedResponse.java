package com.etra.cti.application.apikey.dto;

import com.etra.cti.application.apikey.ApiKeyStatus;
import com.etra.cti.application.apikey.ApplicationApiKey;

import java.time.Instant;
import java.util.UUID;

public record ApiKeyCreatedResponse(
    UUID id,
    UUID applicationId,
    String name,
    String keyPrefix,
    String rawApiKey,
    ApiKeyStatus status,
    Instant createdAt,
    Instant expiresAt
) {
    public static ApiKeyCreatedResponse of(ApplicationApiKey key, String rawApiKey) {
        return new ApiKeyCreatedResponse(
            key.getId(),
            key.getApplicationId(),
            key.getName(),
            key.getKeyPrefix(),
            rawApiKey,
            key.getStatus(),
            key.getCreatedAt(),
            key.getExpiresAt()
        );
    }
}
