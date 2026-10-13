package com.etra.cti.detection.dto;

import com.etra.cti.detection.Detection;
import com.etra.cti.detection.Severity;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record DetectionDto(
    UUID id, UUID eventId, String ruleId, String type,
    Severity severity, BigDecimal confidence, Map<String, Object> details
) {
    public static DetectionDto from(Detection d) {
        return new DetectionDto(d.getId(), d.getEventId(), d.getRuleId(), d.getType(),
            d.getSeverity(), d.getConfidence(), d.getDetails());
    }
}
