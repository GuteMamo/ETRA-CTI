package com.etra.cti.detection.rules;

import com.etra.cti.detection.Detection;
import com.etra.cti.detection.DetectionRule;
import com.etra.cti.detection.Severity;
import com.etra.cti.event.SecurityEvent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Detects mobile API enumeration: probing common mobile endpoints.
 */
@Component
public class MobileApiEnumerationRule implements DetectionRule {

    private static final String[] MOBILE_PATHS = {
        "/api/mobile/", "/api/v1/mobile/", "/api/app/", "/mobile/v1/"
    };

    @Override
    public String id() { return "ETRA-MOBILE-ENUM-001"; }

    @Override
    public Optional<Detection> evaluate(SecurityEvent event) {
        String path = event.getPath() == null ? "" : event.getPath().toLowerCase();
        for (String p : MOBILE_PATHS) {
            if (path.contains(p)) {
                Detection d = new Detection();
                d.setEventId(event.getId());
                d.setRuleId(id());
                d.setType("MOBILE_API_ENUMERATION");
                d.setSeverity(Severity.LOW);
                d.setConfidence(new BigDecimal("0.600"));
                d.setDetails(Map.of(
                    "path", path,
                    "matchedPrefix", p,
                    "sourceIp", String.valueOf(event.getSourceIp())
                ));
                return Optional.of(d);
            }
        }
        return Optional.empty();
    }
}
