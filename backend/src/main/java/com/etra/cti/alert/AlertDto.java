package com.etra.cti.alert;

import com.etra.cti.detection.Severity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AlertDto(
    UUID id,
    String reference,
    UUID detectionId,
    UUID eventId,
    UUID applicationId,
    String title,
    String description,
    Severity severity,
    BigDecimal confidence,
    BigDecimal riskScore,
    AlertStatus status,
    UUID acknowledgedBy,
    Instant acknowledgedAt,
    String dispositionNotes,
    UUID dispositionBy,
    Instant dispositionAt,
    Instant createdAt,
    Instant updatedAt
) {
    public static AlertDto from(Alert a) {
        return new AlertDto(
            a.getId(),
            a.getReference(),
            a.getDetectionId(),
            a.getEventId(),
            a.getApplicationId(),
            a.getTitle(),
            a.getDescription(),
            a.getSeverity(),
            a.getConfidence(),
            a.getRiskScore(),
            a.getStatus(),
            a.getAcknowledgedBy(),
            a.getAcknowledgedAt(),
            a.getDispositionNotes(),
            a.getDispositionBy(),
            a.getDispositionAt(),
            a.getCreatedAt(),
            a.getUpdatedAt()
        );
    }
}
