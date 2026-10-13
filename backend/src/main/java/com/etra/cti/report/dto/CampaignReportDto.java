package com.etra.cti.report.dto;

import com.etra.cti.application.dto.ApplicationDto;
import com.etra.cti.campaign.dto.CampaignDto;
import com.etra.cti.campaign.dto.CampaignEventDto;
import com.etra.cti.finding.dto.FindingDto;

import java.time.Instant;
import java.util.List;

public record CampaignReportDto(
    String reportType,
    Instant generatedAt,
    String executiveSummary,
    CampaignDto campaign,
    ApplicationDto application,
    List<CampaignEventDto> timeline,
    List<FindingDto> findings
) {}
