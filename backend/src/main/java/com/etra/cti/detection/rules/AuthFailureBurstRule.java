package com.etra.cti.detection.rules;

import com.etra.cti.detection.Detection;
import com.etra.cti.detection.DetectionRule;
import com.etra.cti.detection.Severity;
import com.etra.cti.event.SecurityEvent;
import com.etra.cti.event.SecurityEventRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AuthFailureBurstRule implements DetectionRule {

    private static final Duration WINDOW = Duration.ofSeconds(60);
    private static final int THRESHOLD = 3;

    private final SecurityEventRepository eventRepo;

    public AuthFailureBurstRule(SecurityEventRepository eventRepo) {
        this.eventRepo = eventRepo;
    }

    @Override
    public String id() {
        return "AUTH_FAILURE_BURST";
    }

    @Override
    public Optional<Detection> evaluate(SecurityEvent event) {
        if (event == null || event.getStatusCode() == null || (event.getStatusCode() != 401 && event.getStatusCode() != 403)) {
            return Optional.empty();
        }
        if (event.getSourceIp() == null || event.getApplicationId() == null || event.getEventTime() == null) {
            return Optional.empty();
        }

        String path = event.getPath() == null ? "" : event.getPath().toLowerCase();
        if (!path.contains("login") && !path.contains("auth") && !path.contains("signin")) {
            return Optional.empty();
        }

        Instant windowStart = event.getEventTime().minus(WINDOW);
        List<SecurityEvent> recent = eventRepo.findByApplicationIdAndSourceIpAndEventTimeAfter(
            event.getApplicationId(), event.getSourceIp(), windowStart);

        long failures = recent.stream()
            .filter(e -> e.getStatusCode() != null && (e.getStatusCode() == 401 || e.getStatusCode() == 403))
            .count();

        if (failures < THRESHOLD) {
            return Optional.empty();
        }

        List<String> eventIds = recent.stream()
            .filter(e -> e.getStatusCode() != null && (e.getStatusCode() == 401 || e.getStatusCode() == 403))
            .map(e -> e.getId().toString())
            .toList();

        Detection d = new Detection();
        d.setEventId(event.getId());
        d.setRuleId(id());
        d.setType("AUTH_FAILURE_BURST");
        d.setSeverity(Severity.HIGH);
        d.setConfidence(new BigDecimal("0.850"));
        d.setDetails(Map.of(
            "ruleCode", id(),
            "title", "Repeated Failed Logins",
            "reason", failures + " failed authentication events from source " + event.getSourceIp() + " within 60 seconds",
            "failedCount", failures,
            "threshold", THRESHOLD,
            "windowSeconds", 60,
            "sourceIp", event.getSourceIp(),
            "eventIds", eventIds,
            "detectedAt", Instant.now().toString()
        ));

        return Optional.of(d);
    }
}
