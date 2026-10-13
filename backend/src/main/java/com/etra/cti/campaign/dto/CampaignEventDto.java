package com.etra.cti.campaign.dto;

import com.etra.cti.campaign.AttackStage;
import com.etra.cti.campaign.CampaignEvent;

import java.time.Instant;
import java.util.UUID;

public record CampaignEventDto(
    UUID id, UUID campaignId, UUID eventId,
    AttackStage stage, String detectionType,
    Integer sequenceNo, Instant createdAt
) {
    public static CampaignEventDto from(CampaignEvent e) {
        return new CampaignEventDto(e.getId(), e.getCampaignId(), e.getEventId(),
            e.getStage(), e.getDetectionType(), e.getSequenceNo(), e.getCreatedAt());
    }
}
