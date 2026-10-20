package com.etra.cti.report;

import com.etra.cti.report.dto.CampaignReportDto;
import com.etra.cti.report.dto.FindingReportDto;
import com.etra.cti.report.dto.SecuritySummaryReportDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST','DEVELOPER','VIEWER')")
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public SecuritySummaryReportDto summaryReport() {
        return service.securitySummaryReport();
    }

    @GetMapping("/finding/{id}")
    public FindingReportDto findingReport(@PathVariable UUID id) {
        return service.findingReport(id);
    }

    @GetMapping("/campaign/{id}")
    public CampaignReportDto campaignReport(@PathVariable UUID id) {
        return service.campaignReport(id);
    }
}
