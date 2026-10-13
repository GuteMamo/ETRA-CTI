package com.etra.cti.campaign;

import com.etra.cti.detection.Detection;
import com.etra.cti.detection.DetectionRepository;
import com.etra.cti.event.SecurityEvent;
import com.etra.cti.event.SecurityEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CorrelationEngine {

    private static final Logger log = LoggerFactory.getLogger(CorrelationEngine.class);
    private static final Duration WINDOW = Duration.ofMinutes(5);
    private static final int MIN_EVENTS_FOR_CAMPAIGN = 2;

    private final AttackCampaignRepository campaigns;
    private final CampaignEventRepository campaignEvents;
    private final SecurityEventRepository events;
    private final DetectionRepository detections;

    public CorrelationEngine(AttackCampaignRepository campaigns,
                             CampaignEventRepository campaignEvents,
                             SecurityEventRepository events,
                             DetectionRepository detections) {
        this.campaigns = campaigns;
        this.campaignEvents = campaignEvents;
        this.events = events;
        this.detections = detections;
    }

    /**
     * Called after a new event is ingested. Tries to attach it to an active
     * campaign or creates a new one if enough related events exist.
     */
    @Transactional
    public Optional<AttackCampaign> correlate(SecurityEvent event) {
        if (event.getSourceIp() == null || event.getApplicationId() == null) {
            return Optional.empty();
        }

        // Stage 1 - try to extend an active campaign from the same (app, sourceIp)
        Instant activeWindow = event.getEventTime().minus(WINDOW);
        Optional<AttackCampaign> existing =
            campaigns.findFirstByApplicationIdAndSourceIpAndLastSeenAfterOrderByLastSeenDesc(
                event.getApplicationId(), event.getSourceIp(), activeWindow);

        if (existing.isPresent()) {
            AttackCampaign c = existing.get();
            attachEvent(c, event);
            log.info(">>> Extended campaign {} with event {}", c.getReference(), event.getId());
            return Optional.of(c);
        }

        // Stage 2 - look for a burst of events from same (app, ip) in window
        Instant from = event.getEventTime().minus(WINDOW);
        Instant to   = event.getEventTime().plus(WINDOW);

        List<SecurityEvent> window = events.findByApplicationIdAndSourceIpAndEventTimeBetween(
            event.getApplicationId(), event.getSourceIp(), from, to);

        if (window.size() < MIN_EVENTS_FOR_CAMPAIGN) {
            return Optional.empty();
        }

        // Stage 3 - create a new campaign
        AttackCampaign c = new AttackCampaign();
        c.setReference(nextReference());
        c.setApplicationId(event.getApplicationId());
        c.setSourceIp(event.getSourceIp());
        c.setTitle("Multi-stage attack from " + event.getSourceIp());
        c.setRiskBand("HIGH");
        c.setFirstSeen(window.get(0).getEventTime());
        c.setLastSeen(window.get(window.size() - 1).getEventTime());
        c.setStatus("OPEN");
        campaigns.save(c);

        int seq = 1;
        for (SecurityEvent e : window) {
            attachEventInternal(c, e, seq++);
        }
        c.setEventCount(window.size());
        campaigns.save(c);

        log.info(">>> Created campaign {} with {} events for app {} from {}",
            c.getReference(), c.getEventCount(), c.getApplicationId(), c.getSourceIp());
        return Optional.of(c);
    }

    private void attachEvent(AttackCampaign c, SecurityEvent event) {
        if (campaignEvents.existsByCampaignIdAndEventId(c.getId(), event.getId())) {
            return;
        }
        long count = campaignEvents.countByCampaignId(c.getId());
        attachEventInternal(c, event, (int) count + 1);
        c.setEventCount((int) count + 1);
        if (event.getEventTime().isAfter(c.getLastSeen())) {
            c.setLastSeen(event.getEventTime());
        }
        campaigns.save(c);
    }

    private void attachEventInternal(AttackCampaign c, SecurityEvent e, int seq) {
        CampaignEvent ce = new CampaignEvent();
        ce.setCampaignId(c.getId());
        ce.setEventId(e.getId());
        ce.setStage(classifyStage(e));
        ce.setDetectionType(firstDetectionTypeFor(e.getId()));
        ce.setSequenceNo(seq);
        campaignEvents.save(ce);
    }

    private AttackStage classifyStage(SecurityEvent e) {
        String path = e.getPath() == null ? "" : e.getPath().toLowerCase();
        if (path.contains("login") || path.contains("auth") || path.contains("signin")
            || (e.getStatusCode() != null && (e.getStatusCode() == 401 || e.getStatusCode() == 403))) {
            return AttackStage.AUTH_ABUSE;
        }
        if (path.contains("scan") || path.contains("recon") || path.contains("robots")) {
            return AttackStage.RECON;
        }
        if (path.contains("admin") || path.contains("users") || path.contains("accounts")) {
            return AttackStage.DATA_ACCESS;
        }
        return AttackStage.EXPLOITATION;
    }

    private String firstDetectionTypeFor(UUID eventId) {
        List<Detection> dets = detections.findByEventId(eventId);
        return dets.isEmpty() ? null : dets.get(0).getType();
    }

    private String nextReference() {
        long n = campaigns.count() + 1;
        return String.format("AC-%05d", n);
    }
}
