package com.etra.cti.campaign;

import com.etra.cti.campaign.dto.CampaignDto;
import com.etra.cti.campaign.dto.CampaignEventDto;
import com.etra.cti.common.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CampaignService {

    private final AttackCampaignRepository campaigns;
    private final CampaignEventRepository campaignEvents;

    public CampaignService(AttackCampaignRepository campaigns,
                           CampaignEventRepository campaignEvents) {
        this.campaigns = campaigns;
        this.campaignEvents = campaignEvents;
    }

    public List<CampaignDto> listAll() {
        return campaigns.findAllByOrderByLastSeenDesc().stream().map(CampaignDto::from).toList();
    }

    public CampaignDto get(UUID id) {
        AttackCampaign c = campaigns.findById(id)
            .orElseThrow(() -> new NotFoundException("Campaign not found"));
        return CampaignDto.from(c);
    }

    public List<CampaignEventDto> timeline(UUID campaignId) {
        campaigns.findById(campaignId)
            .orElseThrow(() -> new NotFoundException("Campaign not found"));
        return campaignEvents.findByCampaignIdOrderBySequenceNoAsc(campaignId)
            .stream().map(CampaignEventDto::from).toList();
    }
}
