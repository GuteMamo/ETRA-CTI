package com.etra.cti.alert;

import com.etra.cti.application.Application;
import com.etra.cti.application.ApplicationRepository;
import com.etra.cti.audit.AuditService;
import com.etra.cti.common.exception.NotFoundException;
import com.etra.cti.detection.Detection;
import com.etra.cti.event.SecurityEvent;
import com.etra.cti.finding.RiskEngine;
import com.etra.cti.auth.JwtAuthFilter;
import com.etra.cti.organization.OrganizationService;
import com.etra.cti.organization.dto.OrganizationDto;
import com.etra.cti.project.Project;
import com.etra.cti.project.ProjectRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository alertRepo;
    private final ApplicationRepository appRepo;
    private final ProjectRepository projectRepo;
    private final OrganizationService orgService;
    private final RiskEngine riskEngine;
    private final AuditService audit;

    public AlertService(AlertRepository alertRepo,
                        ApplicationRepository appRepo,
                        ProjectRepository projectRepo,
                        OrganizationService orgService,
                        RiskEngine riskEngine,
                        AuditService audit) {
        this.alertRepo = alertRepo;
        this.appRepo = appRepo;
        this.projectRepo = projectRepo;
        this.orgService = orgService;
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

    public Page<AlertDto> list(AlertStatus status, UUID applicationId, Pageable pageable, JwtAuthFilter.AuthedUser caller) {
        List<UUID> allowedAppIds = null;
        if (caller != null && !"ADMIN".equalsIgnoreCase(caller.role()) && !orgService.isPlatformStaff(caller.id())) {
            List<UUID> orgIds = orgService.listMine(caller.id()).stream()
                .map(OrganizationDto::id)
                .toList();
            if (orgIds.isEmpty()) {
                if (applicationId != null) {
                    throw new AccessDeniedException("Not authorized for application " + applicationId);
                }
                return Page.empty(pageable);
            }
            allowedAppIds = orgIds.stream()
                .flatMap(orgId -> projectRepo.findByOrganizationId(orgId).stream())
                .flatMap(project -> appRepo.findByProjectId(project.getId()).stream())
                .map(Application::getId)
                .distinct()
                .toList();
            if (allowedAppIds.isEmpty()) {
                if (applicationId != null) {
                    throw new AccessDeniedException("Not authorized for application " + applicationId);
                }
                return Page.empty(pageable);
            }
            if (applicationId != null && !allowedAppIds.contains(applicationId)) {
                throw new AccessDeniedException("Not authorized for application " + applicationId);
            }
        }

        final List<UUID> scopedAppIds = allowedAppIds;
        Specification<Alert> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (applicationId != null) {
                predicates.add(cb.equal(root.get("applicationId"), applicationId));
            } else if (scopedAppIds != null) {
                predicates.add(root.get("applicationId").in(scopedAppIds));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (query != null) {
                query.orderBy(cb.desc(root.get("createdAt")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return alertRepo.findAll(spec, pageable).map(AlertDto::from);
    }

    public Page<AlertDto> list(AlertStatus status, UUID applicationId, Pageable pageable) {
        return list(status, applicationId, pageable, null);
    }

    public AlertDto get(UUID id, JwtAuthFilter.AuthedUser caller) {
        Alert a = alertRepo.findById(id).orElseThrow(() -> new NotFoundException("Alert not found"));
        validateAccess(a, caller);
        return AlertDto.from(a);
    }

    public AlertDto get(UUID id) {
        return get(id, null);
    }

    @Transactional
    public AlertDto acknowledge(UUID id, JwtAuthFilter.AuthedUser caller) {
        Alert a = alertRepo.findById(id).orElseThrow(() -> new NotFoundException("Alert not found"));
        validateAccess(a, caller);
        UUID userId = caller != null ? caller.id() : null;
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
    public AlertDto triage(UUID id, TriageAlertRequest req, JwtAuthFilter.AuthedUser caller) {
        Alert a = alertRepo.findById(id).orElseThrow(() -> new NotFoundException("Alert not found"));
        validateAccess(a, caller);
        UUID userId = caller != null ? caller.id() : null;
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

    private void validateAccess(Alert a, JwtAuthFilter.AuthedUser caller) {
        if (caller == null || "ADMIN".equalsIgnoreCase(caller.role()) || orgService.isPlatformStaff(caller.id())) {
            return;
        }
        if (a.getApplicationId() == null) {
            return;
        }
        Application app = appRepo.findById(a.getApplicationId())
            .orElseThrow(() -> new NotFoundException("Application not found"));
        Project project = projectRepo.findById(app.getProjectId())
            .orElseThrow(() -> new NotFoundException("Project not found"));
        orgService.requireMember(project.getOrganizationId(), caller.id());
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
