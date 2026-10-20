package com.etra.cti.mobile;

import com.etra.cti.application.Application;
import com.etra.cti.application.ApplicationRepository;
import com.etra.cti.audit.AuditService;
import com.etra.cti.common.exception.NotFoundException;
import com.etra.cti.detection.Severity;
import com.etra.cti.finding.Finding;
import com.etra.cti.finding.FindingRepository;
import com.etra.cti.finding.FindingStatus;
import com.etra.cti.finding.RiskEngine;
import com.etra.cti.finding.FindingService;
import com.etra.cti.detection.Severity;
import com.etra.cti.integration.ImportedFinding;
import com.etra.cti.mobile.dto.MobileAppDto;
import com.etra.cti.mobile.dto.RegisterMobileAppRequest;
import com.etra.cti.mobile.parsers.MobSFParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MobileAppService {

    private static final Logger log = LoggerFactory.getLogger(MobileAppService.class);

    private final MobileAppRepository mobileApps;
    private final ApplicationRepository applications;
    private final FindingRepository findings;
    private final RiskEngine riskEngine;
    private final FindingService findingService;
    private final AuditService audit;
    private final MobSFParser mobSFParser;

    public MobileAppService(MobileAppRepository mobileApps,
                            ApplicationRepository applications,
                            FindingRepository findings,
                            RiskEngine riskEngine,
                            FindingService findingService,
                            AuditService audit,
                            MobSFParser mobSFParser) {
        this.mobileApps = mobileApps;
        this.applications = applications;
        this.findings = findings;
        this.riskEngine = riskEngine;
        this.findingService = findingService;
        this.audit = audit;
        this.mobSFParser = mobSFParser;
    }

    @Transactional
    public MobileAppDto register(RegisterMobileAppRequest req, UUID userId) {
        Application app = applications.findById(req.applicationId())
            .orElseThrow(() -> new NotFoundException("Application not found"));

        if (mobileApps.existsByApplicationIdAndSha256(app.getId(), req.sha256())) {
            throw new IllegalArgumentException("This mobile build is already registered (sha256 exists)");
        }

        MobileApp m = new MobileApp();
        m.setApplicationId(app.getId());
        m.setPlatform(req.platform());
        m.setPackageName(req.packageName());
        m.setVersionName(req.versionName());
        m.setVersionCode(req.versionCode());
        m.setFileName(req.fileName());
        m.setSizeBytes(req.sizeBytes());
        m.setSha256(req.sha256());
        m.setSigned(req.signed());
        m.setMinSdk(req.minSdk());
        m.setTargetSdk(req.targetSdk());
        m.setPermissions(req.permissions());
        m.setCreatedBy(userId);
        mobileApps.save(m);

        audit.record(userId, "MOBILE_APP_REGISTERED", "MobileApp", m.getId().toString(),
            Map.of("packageName", req.packageName(), "platform", req.platform().name()));

        log.info(">>> Mobile app registered: {} ({})", req.packageName(), req.platform());
        return MobileAppDto.from(m);
    }

    public List<MobileAppDto> list() {
        return mobileApps.findAllByOrderByCreatedAtDesc().stream().map(MobileAppDto::from).toList();
    }

    public List<MobileAppDto> listByApplication(UUID appId) {
        return mobileApps.findByApplicationIdOrderByCreatedAtDesc(appId)
            .stream().map(MobileAppDto::from).toList();
    }

    public MobileAppDto get(UUID id) {
        MobileApp m = mobileApps.findById(id)
            .orElseThrow(() -> new NotFoundException("Mobile app not found"));
        return MobileAppDto.from(m);
    }

    @Transactional
    public Map<String, Object> importMobSF(UUID mobileAppId, MultipartFile file, UUID userId) {
        MobileApp m = mobileApps.findById(mobileAppId)
            .orElseThrow(() -> new NotFoundException("Mobile app not found"));
        Application app = applications.findById(m.getApplicationId())
            .orElseThrow(() -> new NotFoundException("Application not found"));

        try (InputStream in = file.getInputStream()) {
            MobSFParser.MobSFReport report = mobSFParser.parse(in);

            // Enrich the mobile app record with MobSF metadata
            if (report.packageName() != null)  m.setPackageName(report.packageName());
            if (report.versionName() != null)  m.setVersionName(report.versionName());
            if (report.versionCode() != null)  m.setVersionCode(report.versionCode());
            if (report.minSdk() != null)       m.setMinSdk(report.minSdk());
            if (report.targetSdk() != null)    m.setTargetSdk(report.targetSdk());
            if (report.signed() != null)       m.setSigned(report.signed());
            if (report.permissions() != null && !report.permissions().isEmpty()) {
                m.setPermissions(report.permissions());
            }
            mobileApps.save(m);

            // Create findings from MobSF
            int created = 0;
            for (ImportedFinding p : report.findings()) {
                Finding f = new Finding();
                f.setReference(findingService.nextReference());
                f.setApplicationId(app.getId());
                f.setTitle(truncate(p.title(), 255));
                f.setDescription(p.description());
                f.setEndpoint(p.endpoint());
                f.setParameter(p.parameter());
                f.setSeverity(p.severity());
                f.setConfidence(new BigDecimal("0.850"));
                f.setSource("MOBSF");
                f.setSourceDisplay("MobSF (Mobile Security Framework)");
                f.setStatus(FindingStatus.OPEN);
                f.setCreatedBy(userId);

                var risk = riskEngine.calculate(p.severity(), f.getConfidence(), app, 1.0);
                f.setRiskScore(risk.score());
                f.setRiskBand(risk.band());
                findings.saveAndFlush(f);
                created++;
            }

            audit.record(userId, "MOBSF_IMPORT", "MobileApp", mobileAppId.toString(),
                Map.of("findingsCreated", created));

            log.info(">>> MobSF import complete: {} findings for {}", created, m.getPackageName());

            return Map.of(
                "mobileAppId", mobileAppId,
                "packageName", m.getPackageName(),
                "findingsCreated", created,
                "permissionsCount", m.getPermissions() == null ? 0 : m.getPermissions().size()
            );
        } catch (Exception e) {
            log.error("MobSF import failed", e);
            throw new IllegalArgumentException("MobSF import failed: " + e.getMessage(), e);
        }
    }

    public List<Finding> findingsFor(UUID mobileAppId) {
        MobileApp m = mobileApps.findById(mobileAppId)
            .orElseThrow(() -> new NotFoundException("Mobile app not found"));
        return findings.findByApplicationIdOrderByRiskScoreDesc(m.getApplicationId(),
                org.springframework.data.domain.PageRequest.of(0, 200))
            .getContent();
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
