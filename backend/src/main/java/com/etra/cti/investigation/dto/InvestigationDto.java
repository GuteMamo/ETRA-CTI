package com.etra.cti.investigation.dto;

import com.etra.cti.investigation.*;

import java.time.Instant;
import java.util.UUID;

public record InvestigationDto(
    UUID id, String reference, UUID campaignId, UUID applicationId,
    String title, String description,
    InvestigationStatus status, InvestigationPriority priority,
    UUID assignedTo, UUID createdBy, Instant createdAt
) {
    public static InvestigationDto from(Investigation i) {
        return new InvestigationDto(i.getId(), i.getReference(), i.getCampaignId(), i.getApplicationId(),
            i.getTitle(), i.getDescription(), i.getStatus(), i.getPriority(),
            i.getAssignedTo(), i.getCreatedBy(), i.getCreatedAt());
    }
}
