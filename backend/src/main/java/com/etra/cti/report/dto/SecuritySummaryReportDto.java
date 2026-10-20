package com.etra.cti.report.dto;

import java.time.Instant;
import java.util.Map;

public record SecuritySummaryReportDto(
    String reportType,
    Instant generatedAt,
    long totalApplications,
    long totalFindings,
    long criticalFindings,
    long highFindings,
    long overdueFindings,
    long activeCampaigns,
    double averageRiskScore,
    Map<String, Long> severityDistribution,
    Map<String, Long> statusDistribution
) {}
