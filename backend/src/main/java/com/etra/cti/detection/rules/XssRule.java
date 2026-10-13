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
public class XssRule implements DetectionRule {

    private static final Pattern PATTERN = Pattern.compile(
        "(?i)(<script[^>]*>|javascript:|onerror\\s*=|onload\\s*=|" +
        "<img[^>]+src\\s*=\\s*['\"]?javascript|" +
        "<iframe|document\\.cookie|alert\\s*\\(|eval\\s*\\()"
    );

    @Override
    public String id() { return "ETRA-XSS-001"; }

    @Override
    public Optional<Detection> evaluate(SecurityEvent event) {
        String hay = String.valueOf(event.getNormalized().getOrDefault("query", ""))
                   + " " + (event.getPath() == null ? "" : event.getPath());
        var m = PATTERN.matcher(hay);
        if (!m.find()) return Optional.empty();

        Detection d = new Detection();
        d.setEventId(event.getId());
        d.setRuleId(id());
        d.setType("XSS_INDICATOR");
        d.setSeverity(Severity.HIGH);
        d.setConfidence(new BigDecimal("0.850"));
        d.setDetails(Map.of(
            "matched", m.group(),
            "endpoint", String.valueOf(event.getNormalized().get("endpoint")),
            "sourceIp", String.valueOf(event.getSourceIp())
        ));
        return Optional.of(d);
    }
}
