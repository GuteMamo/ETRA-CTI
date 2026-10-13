package com.etra.cti.investigation.dto;

import com.etra.cti.campaign.dto.CampaignDto;
import com.etra.cti.campaign.dto.CampaignEventDto;
import com.etra.cti.finding.dto.FindingDto;

import java.util.List;

public record InvestigationSummaryDto(
    InvestigationDto investigation,
    CampaignDto campaign,
    List<CampaignEventDto> timeline,
    List<FindingDto> findings,
    List<NoteDto> notes
) {}
