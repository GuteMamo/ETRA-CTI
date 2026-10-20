package com.etra.cti.alert;

import com.etra.cti.application.Application;
import com.etra.cti.application.ApplicationRepository;
import com.etra.cti.audit.AuditService;
import com.etra.cti.common.exception.NotFoundException;
import com.etra.cti.detection.Detection;
import com.etra.cti.event.SecurityEvent;
import com.etra.cti.finding.RiskEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository alertRepo;
    private final ApplicationRepository appRepo;
    private final RiskEngine riskEngine;
    private final AuditService audit;

    public AlertService(AlertRepository alertRepo,
                        ApplicationRepository appRepo,
                        RiskEngine riskEngine,
                        AuditService audit) {
        this.alertRepo = alertRepo;
        this.appRepo = appRepo;
        this.riskEngine = riskEngine;
        this.audit = audit;
    }

    @Transactional
    public Alert createFromDetection(Detection d, SecurityEvent e) {
        if (d.getId() != null && alertRepo.existsByDetectionId(d.getId())) {
            return null;
        }

        Application app = appRepo.findById(e.getApplicationId()).orElse(null);

        Alert a = new Alert();
        a.setReference(nextReference());
        a.setDetectionId(d.getId());
        a.setEventId(e.getId());
        a.setApplicationId(e.getApplicationId());
        a.setTitle(formatTitle(d.getType()));
        a.setDescription(formatDescription(d, e));
        a.setSeverity(d.getSeverity());
        a.setConfidence(d.getConfidence());
        a.setStatus(AlertStatus.OPEN);

        if (app != null) {
            var risk = riskEngine.calculate(d.getSeverity(), d.getConfidence(), app, 1.0);
            a.setRiskScore(risk.score());
        }

        alertRepo.save(a);
        log.info(">>> Alert {} created for detection {} on event {} (severity={})",
            a.getReference(), d.getId(), e.getId(), a.getSeverity());

        audit.record(null, "ALERT_CREATED", "Alert", a.getId().toString(),
            Map.of("reference", a.getReference(),
                   "severity", a.getSeverity().name(),
                   "ruleId", String.valueOf(d.getRuleId())));

        return a;
    }

    public Page<AlertDto> list(AlertStatus status, UUID applicationId, Pageable pageable) {
        if (applicationId != null && status != null) {
            return alertRepo.findByApplicationIdAndStatusOrderByCreatedAtDesc(applicationId, status, pageable).map(AlertDto::from);
        } else if (applicationId != null) {
            return alertRepo.findByApplicationIdOrderByCreatedAtDesc(applicationId, pageable).map(AlertDto::from);
        } else if (status != null) {
            return alertRepo.findByStatusOrderByCreatedAtDesc(status, pageable).map(AlertDto::from);
        } else {
            return alertRepo.findAllByOrderByCreatedAtDesc(pageable).map(AlertDto::from);
        }
    }

    public AlertDto get(UUID id) {
        Alert a = alertRepo.findById(id).orElseThrow(() -> new NotFoundException("Alert not found"));
        return AlertDto.from(a);
    }

    @Transactional
    public AlertDto acknowledge(UUID id, UUID userId) {
        Alert a = alertRepo.findById(id).orElseThrow(() -> new NotFoundException("Alert not found"));
        a.setStatus(AlertStatus.ACKNOWLEDGED);
        a.setAcknowledgedBy(userId);
        a.setAcknowledgedAt(Instant.now());
        alertRepo.save(a);

        audit.record(userId, "ALERT_ACKNOWLEDGED", "Alert", a.getId().toString(),
            Map.of("reference", a.getReference(), "acknowledgedBy", String.valueOf(userId)));

        log.info(">>> Alert {} acknowledged by user {}", a.getReference(), userId);
        return AlertDto.from(a);
    }

    @Transactional
    public AlertDto triage(UUID id, TriageAlertRequest req, UUID userId) {
        Alert a = alertRepo.findById(id).orElseThrow(() -> new NotFoundException("Alert not found"));
        AlertStatus old = a.getStatus();
        a.setStatus(req.status());
        a.setDispositionNotes(req.notes());
        a.setDispositionBy(userId);
        a.setDispositionAt(Instant.now());
        if (req.status() == AlertStatus.ACKNOWLEDGED && a.getAcknowledgedAt() == null) {
            a.setAcknowledgedBy(userId);
            a.setAcknowledgedAt(Instant.now());
        }
        alertRepo.save(a);

        audit.recordChange(userId, "ALERT_TRIAGED", "Alert", a.getId().toString(),
            Map.of("status", old.name()),
            Map.of("status", req.status().name(), "notes", req.notes() != null ? req.notes() : ""));

        log.info(">>> Alert {} triaged to {} by user {}: {}",
            a.getReference(), req.status(), userId, req.notes());
        return AlertDto.from(a);
    }

    private String formatTitle(String detectionType) {
        if (detectionType == null) return "Security Alert";
        return detectionType.replace("_", " ");
    }

    private String formatDescription(Detection d, SecurityEvent e) {
        StringBuilder sb = new StringBuilder();
        sb.append("Rule ").append(d.getRuleId()).append(" triggered on ").append(d.getType()).append(". ");
        if (e.getSourceIp() != null) {
            sb.append("Source IP: ").append(e.getSourceIp()).append(". ");
        }
        if (e.getPath() != null) {
            sb.append("Path: ").append(e.getPath()).append(". ");
        }
        if (d.getDetails() != null && !d.getDetails().isEmpty()) {
            sb.append("Details: ").append(d.getDetails());
        }
        return sb.toString();
    }

    private String nextReference() {
        long n = alertRepo.count() + 1;
        return String.format("ALT-%05d", n);
    }
}
