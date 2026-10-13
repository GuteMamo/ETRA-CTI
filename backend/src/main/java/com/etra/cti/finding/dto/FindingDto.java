package com.etra.cti.finding.dto;

import com.etra.cti.detection.Severity;
import com.etra.cti.finding.Finding;
import com.etra.cti.finding.FindingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FindingDto(
    UUID id, String reference,
    UUID applicationId, UUID detectionId,
    String title, String description,
    String endpoint, String parameter,
    Severity severity, BigDecimal confidence,
    FindingStatus status, String source,
    UUID assignedTo,
    BigDecimal riskScore, String riskBand,
    Instant createdAt
) {
    public static FindingDto from(Finding f) {
        return new FindingDto(
            f.getId(), f.getReference(),
            f.getApplicationId(), f.getDetectionId(),
            f.getTitle(), f.getDescription(),
            f.getEndpoint(), f.getParameter(),
            f.getSeverity(), f.getConfidence(),
            f.getStatus(), f.getSource(),
            f.getAssignedTo(),
            f.getRiskScore(), f.getRiskBand(),
            f.getCreatedAt()
        );
    }
}
