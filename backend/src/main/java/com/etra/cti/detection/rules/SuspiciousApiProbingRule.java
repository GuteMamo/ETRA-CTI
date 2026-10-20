package com.etra.cti.detection.rules;

import com.etra.cti.detection.Detection;
import com.etra.cti.detection.DetectionRule;
import com.etra.cti.detection.Severity;
import com.etra.cti.event.SecurityEvent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class SuspiciousApiProbingRule implements DetectionRule {

    private static final Pattern SENSITIVE_ROUTES = Pattern.compile(
        "(?i)(/\\.env|/\\.git|/actuator(/|\\b)|/admin(/|\\b)|/wp-login|/wp-admin|" +
        "/phpmyadmin|/debug|/backup|/\\.aws|/server-status|/web\\.config|/\\.DS_Store|" +
        "/api/v[0-9]+/internal)"
    );

    @Override
    public String id() {
        return "ETRA-PROBE-001";
    }

    @Override
    public Optional<Detection> evaluate(SecurityEvent event) {
        if (event == null) return Optional.empty();
        String path = event.getPath() == null ? "" : event.getPath();
        var m = SENSITIVE_ROUTES.matcher(path);
        if (!m.find()) {
            return Optional.empty();
        }

        Detection d = new Detection();
        d.setEventId(event.getId());
        d.setRuleId(id());
        d.setType("SUSPICIOUS_API_PROBING");
        d.setSeverity(Severity.MEDIUM);
        d.setConfidence(new BigDecimal("0.800"));
        d.setDetails(Map.of(
            "ruleCode", id(),
            "title", "Suspicious API Probing",
            "matchedRoute", m.group(),
            "endpoint", path,
            "sourceIp", String.valueOf(event.getSourceIp() == null ? "unknown" : event.getSourceIp()),
            "reason", "Probing of sensitive administrative or infrastructure route: " + m.group(),
            "eventId", event.getId() != null ? event.getId().toString() : ""
        ));

        return Optional.of(d);
    }
}
