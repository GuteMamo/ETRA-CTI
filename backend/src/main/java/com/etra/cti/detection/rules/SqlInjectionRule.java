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
public class SqlInjectionRule implements DetectionRule {

    private static final Pattern PATTERN = Pattern.compile(
        "(?i)(\\bOR\\b\\s+['\"]?\\d+['\"]?\\s*=\\s*['\"]?\\d+|" +
        "UNION\\s+(ALL\\s+)?SELECT|" +
        "';\\s*(DROP|DELETE|UPDATE|INSERT)|" +
        "\\bSLEEP\\s*\\(|" +
        "\\bBENCHMARK\\s*\\(|" +
        "--\\s|/\\*.*\\*/)"
    );

    @Override
    public String id() { return "ETRA-SQLI-001"; }

    @Override
    public Optional<Detection> evaluate(SecurityEvent event) {
        String hay = (event.getPath() == null ? "" : event.getPath())
                   + " " + String.valueOf(event.getNormalized().getOrDefault("query", ""));
        var m = PATTERN.matcher(hay);
        if (!m.find()) return Optional.empty();

        Detection d = new Detection();
        d.setEventId(event.getId());
        d.setRuleId(id());
        d.setType("SQL_INJECTION_INDICATOR");
        d.setSeverity(Severity.HIGH);
        d.setConfidence(new BigDecimal("0.870"));
        d.setDetails(Map.of(
            "matched", m.group(),
            "endpoint", String.valueOf(event.getNormalized().get("endpoint")),
            "sourceIp", String.valueOf(event.getSourceIp())
        ));
        return Optional.of(d);
    }
}
