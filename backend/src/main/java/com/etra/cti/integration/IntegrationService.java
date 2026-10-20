package com.etra.cti.integration;

import com.etra.cti.application.Application;
import com.etra.cti.application.ApplicationRepository;
import com.etra.cti.asset.Asset;
import com.etra.cti.asset.AssetRepository;
import com.etra.cti.asset.AssetType;
import com.etra.cti.audit.AuditService;
import com.etra.cti.common.exception.NotFoundException;
import com.etra.cti.detection.Severity;
import com.etra.cti.finding.Finding;
import com.etra.cti.finding.FindingRepository;
import com.etra.cti.finding.FindingStatus;
import com.etra.cti.finding.RiskEngine;
import com.etra.cti.finding.FindingService;
import com.etra.cti.integration.dto.ImportDto;
import com.etra.cti.integration.parsers.BurpParser;
import com.etra.cti.integration.parsers.NmapParser;
import com.etra.cti.integration.parsers.ZapParser;
import com.etra.cti.organization.OrganizationService;
import com.etra.cti.project.Project;
import com.etra.cti.project.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class IntegrationService {

    private static final Logger log = LoggerFactory.getLogger(IntegrationService.class);

    private final IntegrationImportRepository imports;
    private final ApplicationRepository apps;
    private final ProjectRepository projects;
    private final OrganizationService orgService;
    private final FindingRepository findings;
    private final AssetRepository assets;
    private final RiskEngine riskEngine;
    private final FindingService findingService;
    private final AuditService audit;
    private final BurpParser burpParser;
    private final ZapParser zapParser;
    private final NmapParser nmapParser;

    public IntegrationService(IntegrationImportRepository imports,
                              ApplicationRepository apps,
                              ProjectRepository projects,
                              OrganizationService orgService,
                              FindingRepository findings,
                              AssetRepository assets,
                              RiskEngine riskEngine,
                              FindingService findingService,
                              AuditService audit,
                              BurpParser burpParser,
                              ZapParser zapParser,
                              NmapParser nmapParser) {
        this.imports = imports;
        this.apps = apps;
        this.projects = projects;
        this.orgService = orgService;
        this.findings = findings;
        this.assets = assets;
        this.riskEngine = riskEngine;
        this.findingService = findingService;
        this.audit = audit;
        this.burpParser = burpParser;
        this.zapParser = zapParser;
        this.nmapParser = nmapParser;
    }

    @Transactional
    public ImportDto importBurp(UUID applicationId, MultipartFile file, UUID userId) {
        return runImport(applicationId, IntegrationSource.BURP, file, userId);
    }

    @Transactional
    public ImportDto importZap(UUID applicationId, MultipartFile file, UUID userId) {
        return runImport(applicationId, IntegrationSource.ZAP, file, userId);
    }

    @Transactional
    public ImportDto importNmap(UUID applicationId, MultipartFile file, UUID userId) {
        return runImport(applicationId, IntegrationSource.NMAP, file, userId);
    }

    private ImportDto runImport(UUID applicationId, IntegrationSource source,
                                MultipartFile file, UUID userId) {
        Application app = apps.findById(applicationId)
            .orElseThrow(() -> new NotFoundException("Application not found"));
        Project project = projects.findById(app.getProjectId())
            .orElseThrow(() -> new NotFoundException("Project not found"));
        orgService.requireMember(project.getOrganizationId(), userId);

        IntegrationImport imp = new IntegrationImport();
        imp.setSource(source);
        imp.setApplicationId(applicationId);
        imp.setFilename(file.getOriginalFilename());
        imp.setSizeBytes(file.getSize());
        imp.setStatus(ImportStatus.RUNNING);
        imp.setImportedBy(userId);
        imports.save(imp);

        Instant start = Instant.now();
        try (InputStream in = file.getInputStream()) {
            switch (source) {
                case BURP -> processFindings(app, imp, burpParser.parse(in));
                case ZAP  -> processFindings(app, imp, zapParser.parse(in));
                case NMAP -> processAssets(imp, nmapParser.parse(in));
                default -> throw new IllegalArgumentException("Unsupported source: " + source);
            }
            imp.setStatus(ImportStatus.COMPLETED);
        } catch (Exception e) {
            log.error("Import failed", e);
            imp.setStatus(ImportStatus.FAILED);
            imp.setErrorMessage(e.getMessage());
        } finally {
            imp.setFinishedAt(Instant.now());
            imp.setDurationMs(Duration.between(start, imp.getFinishedAt()).toMillis());
            imports.save(imp);
        }

        audit.record(userId, "IMPORT_" + source.name(), "Application", applicationId.toString(),
            Map.of("importId", imp.getId().toString(),
                   "findingsCreated", imp.getFindingsCreated(),
                   "assetsCreated", imp.getAssetsCreated(),
                   "status", imp.getStatus().name()));

        log.info(">>> Import {} finished: source={} findings={} duplicates={} assets={} status={}",
            imp.getId(), source, imp.getFindingsCreated(), imp.getFindingsDuplicated(),
            imp.getAssetsCreated(), imp.getStatus());

        return ImportDto.from(imp);
    }

    private void processFindings(Application app, IntegrationImport imp, List<ImportedFinding> parsed) {
        int created = 0;
        int dup = 0;
        for (ImportedFinding p : parsed) {
            if (isDuplicate(app.getId(), p)) {
                dup++;
                continue;
            }
            Finding f = new Finding();
            f.setReference(findingService.nextReference());
            f.setApplicationId(app.getId());
            f.setTitle(truncate(p.title(), 255));
            f.setDescription(p.description());
            f.setEndpoint(truncate(p.endpoint(), 1000));
            f.setParameter(truncate(p.parameter(), 255));
            f.setSeverity(p.severity());
            f.setConfidence(new BigDecimal("0.900"));
            f.setSource(imp.getSource().name());
            f.setSourceDisplay(displayNameFor(imp.getSource().name()));
            f.setStatus(FindingStatus.OPEN);
            f.setCreatedBy(imp.getImportedBy());

            var risk = riskEngine.calculate(p.severity(), f.getConfidence(), app, 1.0);
            f.setRiskScore(risk.score());
            f.setRiskBand(risk.band());
            findings.saveAndFlush(f);

            // Auto-generate remediation for high/critical scanner findings
            if (p.severity() == Severity.HIGH || p.severity() == Severity.CRITICAL) {
                findingService.autoCreateRemediation(f);
            }

            created++;
        }
        imp.setFindingsCreated(created);
        imp.setFindingsDuplicated(dup);
    }

    private void processAssets(IntegrationImport imp, List<NmapParser.NmapHost> hosts) {
        int created = 0;
        for (NmapParser.NmapHost h : hosts) {
            Asset a = new Asset();
            a.setApplicationId(imp.getApplicationId());
            a.setType(AssetType.SERVICE);
            a.setHostname(h.ip());
            a.setPort(h.port());
            a.setProtocol(h.protocol());
            a.setStatus("ACTIVE");
            a.setCriticality("MEDIUM");
            assets.save(a);
            created++;
        }
        imp.setAssetsCreated(created);
    }

    private boolean isDuplicate(UUID appId, ImportedFinding p) {
        return findings.findByApplicationIdOrderByRiskScoreDesc(appId,
                org.springframework.data.domain.PageRequest.of(0, 200))
            .getContent()
            .stream()
            .anyMatch(existing ->
                safeEquals(existing.getTitle(), p.title()) &&
                safeEquals(existing.getEndpoint(), p.endpoint()));
    }

    private boolean safeEquals(String a, String b) {
        return a != null && a.equals(b);
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private String displayNameFor(String source) {
        if (source == null) return "Unknown";
        return switch (source.toUpperCase()) {
            case "BURP"  -> "Burp Suite";
            case "ZAP"   -> "OWASP ZAP";
            case "NMAP"  -> "Nmap";
            case "MOBSF" -> "MobSF (Mobile Security Framework)";
            default      -> source;
        };
    }

    public List<ImportDto> listAll() {
        return imports.findAllByOrderByStartedAtDesc().stream().map(ImportDto::from).toList();
    }

    public ImportDto get(UUID id) {
        IntegrationImport imp = imports.findById(id)
            .orElseThrow(() -> new NotFoundException("Import not found"));
        return ImportDto.from(imp);
    }
}
