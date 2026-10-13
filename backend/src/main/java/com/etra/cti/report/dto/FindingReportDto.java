package com.etra.cti.report.dto;

import com.etra.cti.audit.dto.AuditLogDto;
import com.etra.cti.application.dto.ApplicationDto;
import com.etra.cti.evidence.dto.EvidenceDto;
import com.etra.cti.finding.dto.FindingDto;
import com.etra.cti.remediation.dto.RemediationDto;
import com.etra.cti.remediation.dto.RetestDto;

import java.time.Instant;
import java.util.List;

public record FindingReportDto(
    String reportType,
    Instant generatedAt,
    String executiveSummary,
    FindingDto finding,
    ApplicationDto application,
    List<EvidenceDto> evidence,
    RemediationDto remediation,
    List<RetestDto> retests,
    List<AuditLogDto> auditTrail
) {}
