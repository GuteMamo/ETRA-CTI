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
public class PathTraversalRule implements DetectionRule {

    private static final Pattern PATTERN = Pattern.compile(
        "(?i)(\\.\\./|\\.\\.\\\\|%2e%2e%2f|%2e%2e/|" +
        "/etc/passwd|/etc/shadow|" +
        "c:\\\\windows|%00)"
    );

    @Override
    public String id() { return "ETRA-PTRAV-001"; }

    @Override
    public Optional<Detection> evaluate(SecurityEvent event) {
        String hay = (event.getPath() == null ? "" : event.getPath())
                   + " " + String.valueOf(event.getNormalized().getOrDefault("query", ""));
        var m = PATTERN.matcher(hay);
        if (!m.find()) return Optional.empty();

        Detection d = new Detection();
        d.setEventId(event.getId());
        d.setRuleId(id());
        d.setType("PATH_TRAVERSAL_INDICATOR");
        d.setSeverity(Severity.HIGH);
        d.setConfidence(new BigDecimal("0.830"));
        d.setDetails(Map.of(
            "matched", m.group(),
            "endpoint", String.valueOf(event.getNormalized().get("endpoint")),
            "sourceIp", String.valueOf(event.getSourceIp())
        ));
        return Optional.of(d);
    }
}
