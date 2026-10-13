package com.etra.cti.detection.rules;

import com.etra.cti.detection.Detection;
import com.etra.cti.detection.DetectionRule;
import com.etra.cti.detection.Severity;
import com.etra.cti.event.SecurityEvent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Component
public class AuthAbuseRule implements DetectionRule {

    @Override
    public String id() { return "ETRA-AUTH-001"; }

    @Override
    public Optional<Detection> evaluate(SecurityEvent event) {
        if (event.getStatusCode() == null) return Optional.empty();
        if (event.getStatusCode() != 401 && event.getStatusCode() != 403) return Optional.empty();

        String path = event.getPath() == null ? "" : event.getPath().toLowerCase();
        if (!path.contains("login") && !path.contains("auth") && !path.contains("signin")) {
            return Optional.empty();
        }

        Detection d = new Detection();
        d.setEventId(event.getId());
        d.setRuleId(id());
        d.setType("AUTHENTICATION_ABUSE");
        d.setSeverity(Severity.MEDIUM);
        d.setConfidence(new BigDecimal("0.700"));
        d.setDetails(Map.of(
            "statusCode", event.getStatusCode(),
            "endpoint", path,
            "sourceIp", String.valueOf(event.getSourceIp())
        ));
        return Optional.of(d);
    }
}
