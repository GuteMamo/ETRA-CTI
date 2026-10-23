package com.etra.cti.event;

import com.etra.cti.application.Application;
import com.etra.cti.application.ApplicationRepository;
import com.etra.cti.auth.JwtAuthFilter;
import com.etra.cti.campaign.CorrelationEngine;
import com.etra.cti.common.exception.NotFoundException;
import com.etra.cti.detection.Detection;
import com.etra.cti.detection.DetectionRepository;
import com.etra.cti.detection.DetectionService;
import com.etra.cti.detection.Severity;
import com.etra.cti.event.dto.EventDto;
import com.etra.cti.event.dto.IngestEventRequest;
import com.etra.cti.organization.OrganizationService;
import com.etra.cti.project.Project;
import com.etra.cti.project.ProjectRepository;
import com.etra.cti.threatintel.Ioc;
import com.etra.cti.threatintel.IocType;
import com.etra.cti.threatintel.ThreatIntelService;
import com.etra.cti.organization.dto.OrganizationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class EventService {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    private final SecurityEventRepository events;
    private final ApplicationRepository apps;
    private final ProjectRepository projects;
    private final OrganizationService orgService;
    private final DetectionService detectionService;
    private final CorrelationEngine correlationEngine;
    private final ThreatIntelService threatIntel;
    private final DetectionRepository detections;
    private final com.etra.cti.alert.AlertService alertService;

    public EventService(SecurityEventRepository events,
                        ApplicationRepository apps,
                        ProjectRepository projects,
                        OrganizationService orgService,
                        DetectionService detectionService,
                        CorrelationEngine correlationEngine,
                        ThreatIntelService threatIntel,
                        DetectionRepository detections,
                        com.etra.cti.alert.AlertService alertService) {
        this.events = events;
        this.apps = apps;
        this.projects = projects;
        this.orgService = orgService;
        this.detectionService = detectionService;
        this.correlationEngine = correlationEngine;
        this.threatIntel = threatIntel;
        this.detections = detections;
        this.alertService = alertService;
    }

    @Transactional
    public EventDto ingest(IngestEventRequest req) {
        return ingest(req, null);
    }

    @Transactional
    public EventDto ingest(IngestEventRequest req, JwtAuthFilter.AuthedUser caller) {
        Application app = apps.findById(req.applicationId())
            .orElseThrow(() -> new NotFoundException("Application not found"));

        if (caller != null) {
            if ("APPLICATION".equalsIgnoreCase(caller.role())) {
                if (!req.applicationId().equals(caller.id())) {
                    log.warn(">>> BOLA/IDOR attempt: API key for app {} attempted to submit event for app {}",
                        caller.id(), req.applicationId());
                    throw new AccessDeniedException("API key is not authorized for application " + req.applicationId());
                }
            } else {
                Project p = projects.findById(app.getProjectId())
                    .orElseThrow(() -> new NotFoundException("Project not found"));
                orgService.requireMember(p.getOrganizationId(), caller.id());
            }
        }

        SecurityEvent e = new SecurityEvent();
        e.setApplicationId(app.getId());
        e.setSource(req.source());
        e.setEventTime(req.timestamp());
        e.setSourceIp(req.sourceIp());
        e.setMethod(req.method());
        e.setPath(EventNormalizer.redact(req.path()));
        e.setStatusCode(req.statusCode());
        e.setUserIdentifier(req.userId());
        e.setRawPayload(EventNormalizer.redactPayload(req.metadata()));
        e.setNormalized(EventNormalizer.normalize(req));
        events.save(e);

        // Rule-based detection
        detectionService.analyze(e.getId());

        // Threat intel IOC matching
        checkIoc(e);

        // Correlation
        correlationEngine.correlate(e);

        return EventDto.from(e);
    }

    private void checkIoc(SecurityEvent e) {
        Optional<Ioc> matched = threatIntel.match(IocType.IP, e.getSourceIp());
        if (matched.isEmpty()) return;

        Ioc ioc = matched.get();
        Detection d = new Detection();
        d.setEventId(e.getId());
        d.setRuleId("ETRA-IOC-001");
        d.setType("KNOWN_MALICIOUS_SOURCE");
        d.setSeverity(ioc.getSeverity() == null ? Severity.HIGH : ioc.getSeverity());
        d.setConfidence(new BigDecimal("0.990"));
        d.setDetails(Map.of(
            "iocId", ioc.getId().toString(),
            "iocValue", ioc.getValue(),
            "iocSource", String.valueOf(ioc.getSource()),
            "description", String.valueOf(ioc.getDescription())
        ));
        detections.save(d);
        alertService.createFromDetection(d, e);

        log.warn(">>> IOC MATCH on event {}: source {} is a known malicious indicator ({})",
            e.getId(), e.getSourceIp(), ioc.getSource());
    }

    public Page<EventDto> listByApplication(UUID applicationId, Pageable pageable, JwtAuthFilter.AuthedUser caller) {
        if (caller != null && !"ADMIN".equalsIgnoreCase(caller.role()) && !orgService.isPlatformStaff(caller.id())) {
            Application a = apps.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));
            Project p = projects.findById(a.getProjectId())
                .orElseThrow(() -> new NotFoundException("Project not found"));
            orgService.requireMember(p.getOrganizationId(), caller.id());
        }
        return events.findByApplicationIdOrderByEventTimeDesc(applicationId, pageable).map(EventDto::from);
    }

    public Page<EventDto> listByApplication(UUID applicationId, Pageable pageable) {
        return listByApplication(applicationId, pageable, null);
    }

    public Page<EventDto> listAll(Pageable pageable, JwtAuthFilter.AuthedUser caller) {
        if (caller != null && !"ADMIN".equalsIgnoreCase(caller.role()) && !orgService.isPlatformStaff(caller.id())) {
            List<UUID> orgIds = orgService.listMine(caller.id()).stream()
                .map(OrganizationDto::id)
                .toList();
            if (orgIds.isEmpty()) {
                return Page.empty(pageable);
            }
            List<UUID> appIds = orgIds.stream()
                .flatMap(orgId -> projects.findByOrganizationId(orgId).stream())
                .flatMap(project -> apps.findByProjectId(project.getId()).stream())
                .map(Application::getId)
                .distinct()
                .toList();
            if (appIds.isEmpty()) {
                return Page.empty(pageable);
            }
            return events.findByApplicationIdInOrderByEventTimeDesc(appIds, pageable).map(EventDto::from);
        }
        return events.findAllByOrderByEventTimeDesc(pageable).map(EventDto::from);
    }

    public Page<EventDto> listAll(Pageable pageable) {
        return listAll(pageable, null);
    }

    public EventDto get(UUID id, JwtAuthFilter.AuthedUser caller) {
        SecurityEvent e = events.findById(id)
            .orElseThrow(() -> new NotFoundException("Security event not found"));
        if (caller != null && !"ADMIN".equalsIgnoreCase(caller.role())) {
            Application a = apps.findById(e.getApplicationId())
                .orElseThrow(() -> new NotFoundException("Application not found"));
            Project p = projects.findById(a.getProjectId())
                .orElseThrow(() -> new NotFoundException("Project not found"));
            orgService.requireMember(p.getOrganizationId(), caller.id());
        }
        return EventDto.from(e);
    }

    public EventDto get(UUID id) {
        return get(id, null);
    }
}
