package com.etra.cti.event;

import com.etra.cti.application.Application;
import com.etra.cti.application.ApplicationRepository;
import com.etra.cti.campaign.CorrelationEngine;
import com.etra.cti.common.exception.NotFoundException;
import com.etra.cti.detection.Detection;
import com.etra.cti.detection.DetectionRepository;
import com.etra.cti.detection.DetectionService;
import com.etra.cti.detection.Severity;
import com.etra.cti.event.dto.EventDto;
import com.etra.cti.event.dto.IngestEventRequest;
import com.etra.cti.threatintel.Ioc;
import com.etra.cti.threatintel.IocType;
import com.etra.cti.threatintel.ThreatIntelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class EventService {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    private final SecurityEventRepository events;
    private final ApplicationRepository apps;
    private final DetectionService detectionService;
    private final CorrelationEngine correlationEngine;
    private final ThreatIntelService threatIntel;
    private final DetectionRepository detections;

    public EventService(SecurityEventRepository events,
                        ApplicationRepository apps,
                        DetectionService detectionService,
                        CorrelationEngine correlationEngine,
                        ThreatIntelService threatIntel,
                        DetectionRepository detections) {
        this.events = events;
        this.apps = apps;
        this.detectionService = detectionService;
        this.correlationEngine = correlationEngine;
        this.threatIntel = threatIntel;
        this.detections = detections;
    }

    @Transactional
    public EventDto ingest(IngestEventRequest req) {
        Application app = apps.findById(req.applicationId())
            .orElseThrow(() -> new NotFoundException("Application not found"));

        SecurityEvent e = new SecurityEvent();
        e.setApplicationId(app.getId());
        e.setSource(req.source());
        e.setEventTime(req.timestamp());
        e.setSourceIp(req.sourceIp());
        e.setMethod(req.method());
        e.setPath(req.path());
        e.setStatusCode(req.statusCode());
        e.setUserIdentifier(req.userId());
        e.setRawPayload(req.metadata());
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

        log.warn(">>> IOC MATCH on event {}: source {} is a known malicious indicator ({})",
            e.getId(), e.getSourceIp(), ioc.getSource());
    }

    public Page<EventDto> listByApplication(UUID applicationId, Pageable pageable) {
        return events.findByApplicationIdOrderByEventTimeDesc(applicationId, pageable).map(EventDto::from);
    }
}
