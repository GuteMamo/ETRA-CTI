package com.etra.cti.campaign.dto;

import com.etra.cti.campaign.AttackCampaign;

import java.time.Instant;
import java.util.UUID;

public record CampaignDto(
    UUID id, String reference, UUID applicationId, String sourceIp,
    String title, String riskBand, Integer eventCount,
    Instant firstSeen, Instant lastSeen, String status
) {
    public static CampaignDto from(AttackCampaign c) {
        return new CampaignDto(c.getId(), c.getReference(), c.getApplicationId(), c.getSourceIp(),
            c.getTitle(), c.getRiskBand(), c.getEventCount(),
            c.getFirstSeen(), c.getLastSeen(), c.getStatus());
    }
}
