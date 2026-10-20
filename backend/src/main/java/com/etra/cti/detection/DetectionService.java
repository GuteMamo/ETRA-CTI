package com.etra.cti.detection;

import com.etra.cti.event.SecurityEvent;
import com.etra.cti.event.SecurityEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DetectionService {

    private static final Logger log = LoggerFactory.getLogger(DetectionService.class);

    private final List<DetectionRule> rules;
    private final DetectionRepository detections;
    private final SecurityEventRepository events;
    private final com.etra.cti.alert.AlertService alertService;

    public DetectionService(List<DetectionRule> rules,
                            DetectionRepository detections,
                            SecurityEventRepository events,
                            com.etra.cti.alert.AlertService alertService) {
        this.rules = rules;
        this.detections = detections;
        this.events = events;
        this.alertService = alertService;
        log.info(">>> DetectionService initialized with {} rules: {}",
            rules.size(),
            rules.stream().map(DetectionRule::id).toList());
    }

    @Transactional
    public void analyze(UUID eventId) {
        log.info(">>> analyze() called for event {}", eventId);
        SecurityEvent event = events.findById(eventId).orElse(null);
        if (event == null) {
            log.warn(">>> Event {} not found", eventId);
            return;
        }
        log.info(">>> Evaluating {} rules against event {} (path={})",
            rules.size(), eventId, event.getPath());

        int saved = 0;
        for (DetectionRule rule : rules) {
            var opt = rule.evaluate(event);
            if (opt.isPresent()) {
                Detection d = opt.get();
                log.info(">>> Rule {} matched → saving detection type={}", rule.id(), d.getType());
                detections.save(d);
                alertService.createFromDetection(d, event);
                saved++;
            } else {
                log.debug(">>> Rule {} did not match", rule.id());
            }
        }
        log.info(">>> analyze() complete for event {}: {} detections saved", eventId, saved);
    }

    public List<Detection> forEvent(UUID eventId) {
        return detections.findByEventId(eventId);
    }
}