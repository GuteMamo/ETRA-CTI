package com.etra.cti.report;

import com.etra.cti.application.Application;
import com.etra.cti.application.ApplicationRepository;
import com.etra.cti.application.dto.ApplicationDto;
import com.etra.cti.audit.AuditService;
import com.etra.cti.campaign.AttackCampaign;
import com.etra.cti.campaign.AttackCampaignRepository;
import com.etra.cti.campaign.CampaignEventRepository;
import com.etra.cti.campaign.dto.CampaignDto;
import com.etra.cti.campaign.dto.CampaignEventDto;
import com.etra.cti.common.exception.NotFoundException;
import com.etra.cti.evidence.EvidenceRepository;
import com.etra.cti.evidence.dto.EvidenceDto;
import com.etra.cti.finding.Finding;
import com.etra.cti.finding.FindingRepository;
import com.etra.cti.finding.dto.FindingDto;
import com.etra.cti.remediation.Remediation;
import com.etra.cti.remediation.RemediationRepository;
import com.etra.cti.remediation.RetestRepository;
import com.etra.cti.remediation.dto.RemediationDto;
import com.etra.cti.remediation.dto.RetestDto;
import com.etra.cti.report.dto.CampaignReportDto;
import com.etra.cti.report.dto.FindingReportDto;
import com.etra.cti.auth.JwtAuthFilter;
import com.etra.cti.organization.OrganizationService;
import com.etra.cti.project.Project;
import com.etra.cti.project.ProjectRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ReportService {

    private final FindingRepository findings;
    private final ApplicationRepository apps;
    private final EvidenceRepository evidence;
    private final RemediationRepository remediations;
    private final RetestRepository retests;
    private final AuditService audit;
    private final AttackCampaignRepository campaigns;
    private final CampaignEventRepository campaignEvents;
    private final ProjectRepository projects;
    private final OrganizationService orgService;

    public ReportService(FindingRepository findings,
                         ApplicationRepository apps,
                         EvidenceRepository evidence,
                         RemediationRepository remediations,
                         RetestRepository retests,
                         AuditService audit,
                         AttackCampaignRepository campaigns,
                         CampaignEventRepository campaignEvents,
                         ProjectRepository projects,
                         OrganizationService orgService) {
        this.findings = findings;
        this.apps = apps;
        this.evidence = evidence;
        this.remediations = remediations;
        this.retests = retests;
        this.audit = audit;
        this.campaigns = campaigns;
        this.campaignEvents = campaignEvents;
        this.projects = projects;
        this.orgService = orgService;
    }

    public FindingReportDto findingReport(UUID findingId, JwtAuthFilter.AuthedUser caller) {
        Finding f = findings.findById(findingId)
            .orElseThrow(() -> new NotFoundException("Finding not found"));

        Application app = apps.findById(f.getApplicationId())
            .orElseThrow(() -> new NotFoundException("Application not found"));

        if (caller != null && !"ADMIN".equalsIgnoreCase(caller.role())) {
            Project project = projects.findById(app.getProjectId())
                .orElseThrow(() -> new NotFoundException("Project not found"));
            orgService.requireMember(project.getOrganizationId(), caller.id());
        }

        List<EvidenceDto> evs = evidence.findByFindingId(findingId)
            .stream().map(EvidenceDto::from).toList();

        Remediation rem = remediations.findByFindingId(findingId).orElse(null);
        List<RetestDto> rts = retests.findByFindingIdOrderByCreatedAtDesc(findingId)
            .stream().map(RetestDto::from).toList();

        String summary = buildFindingSummary(f, app, rem, rts);

        return new FindingReportDto(
            "FINDING",
            Instant.now(),
            summary,
            FindingDto.from(f),
            ApplicationDto.from(app),
            evs,
            rem == null ? null : RemediationDto.from(rem),
            rts,
            audit.trail("Finding", findingId.toString())
        );
    }

    public FindingReportDto findingReport(UUID findingId) {
        return findingReport(findingId, null);
    }

    public CampaignReportDto campaignReport(UUID campaignId, JwtAuthFilter.AuthedUser caller) {
        AttackCampaign c = campaigns.findById(campaignId)
            .orElseThrow(() -> new NotFoundException("Campaign not found"));

        Application app = apps.findById(c.getApplicationId()).orElse(null);

        if (caller != null && !"ADMIN".equalsIgnoreCase(caller.role())) {
            if (app != null) {
                Project project = projects.findById(app.getProjectId())
                    .orElseThrow(() -> new NotFoundException("Project not found"));
                orgService.requireMember(project.getOrganizationId(), caller.id());
            }
        }

        List<CampaignEventDto> timeline = campaignEvents
            .findByCampaignIdOrderBySequenceNoAsc(campaignId)
            .stream().map(CampaignEventDto::from).toList();

        List<FindingDto> linkedFindings = findings.findByApplicationIdOrderByRiskScoreDesc(
                c.getApplicationId(), org.springframework.data.domain.PageRequest.of(0, 50))
            .stream()
            .filter(f -> f.getEndpoint() != null && f.getEndpoint().contains("/"))
            .map(FindingDto::from).toList();

        String summary = String.format(
            "Attack campaign %s detected against %s from source IP %s. " +
            "%d events across %d stages (%s). Risk band: %s.",
            c.getReference(),
            app == null ? "unknown application" : app.getName(),
            c.getSourceIp(),
            c.getEventCount(),
            timeline.stream().map(e -> e.stage().name()).distinct().count(),
            timeline.stream().map(e -> e.stage().name()).distinct().reduce((a, b) -> a + ", " + b).orElse("n/a"),
            c.getRiskBand()
        );

        return new CampaignReportDto(
            "CAMPAIGN",
            Instant.now(),
            summary,
            CampaignDto.from(c),
            app == null ? null : ApplicationDto.from(app),
            timeline,
            linkedFindings
        );
    }

    public com.etra.cti.report.dto.SecuritySummaryReportDto securitySummaryReport() {
        List<Finding> allFindings = findings.findAll();
        long totalApps = apps.count();
        long totalFindings = allFindings.size();
        long critical = allFindings.stream().filter(f -> f.getSeverity() == com.etra.cti.detection.Severity.CRITICAL).count();
        long high = allFindings.stream().filter(f -> f.getSeverity() == com.etra.cti.detection.Severity.HIGH).count();
        long overdue = allFindings.stream().filter(f -> f.getDueDate() != null && f.getDueDate().isBefore(Instant.now())
            && f.getStatus() != com.etra.cti.finding.FindingStatus.VERIFIED
            && f.getStatus() != com.etra.cti.finding.FindingStatus.CLOSED).count();
        long activeCampaigns = campaigns.findAll().stream().filter(c -> "OPEN".equalsIgnoreCase(c.getStatus())).count();
        double avgRisk = allFindings.isEmpty() ? 0.0 : allFindings.stream()
            .mapToDouble(f -> f.getRiskScore() != null ? f.getRiskScore().doubleValue() : 0.0).average().orElse(0.0);

        Map<String, Long> sevDist = allFindings.stream()
            .collect(java.util.stream.Collectors.groupingBy(f -> f.getSeverity().name(), java.util.stream.Collectors.counting()));
        Map<String, Long> statusDist = allFindings.stream()
            .collect(java.util.stream.Collectors.groupingBy(f -> f.getStatus().name(), java.util.stream.Collectors.counting()));

        return new com.etra.cti.report.dto.SecuritySummaryReportDto(
            "EXECUTIVE_SECURITY_SUMMARY",
            Instant.now(),
            totalApps,
            totalFindings,
            critical,
            high,
            overdue,
            activeCampaigns,
            Math.round(avgRisk * 10.0) / 10.0,
            sevDist,
            statusDist
        );
    }

    private String buildFindingSummary(Finding f, Application app, Remediation rem, List<RetestDto> rts) {
        String status;
        if (rem == null) {
            status = "no remediation started yet";
        } else {
            status = switch (rem.getStatus()) {
                case VERIFIED -> "remediation was verified after " + rem.getReopenedCount() + " reopened cycle(s)";
                case REOPENED -> "remediation failed retest and was reopened";
                case READY_FOR_RETEST -> "remediation is awaiting retest";
                case IN_PROGRESS -> "remediation is in progress";
                case OPEN -> "remediation is open";
                case FIXED -> "fix was submitted";
            };
        }
        return String.format(
            "%s severity %s finding %s on %s (%s, %s environment). " +
            "Risk score %.1f (%s). Endpoint: %s%s. %d evidence artifact(s). Current state: %s.",
            f.getSeverity().name(),
            f.getTitle(),
            f.getReference(),
            app.getName(),
            app.getType().name(),
            app.getEnvironment().name(),
            f.getRiskScore() == null ? 0.0 : f.getRiskScore().doubleValue(),
            f.getRiskBand(),
            f.getEndpoint() == null ? "n/a" : f.getEndpoint(),
            f.getParameter() == null ? "" : "?" + f.getParameter(),
            evidence.findByFindingId(f.getId()).size(),
            status
        );
    }
}
