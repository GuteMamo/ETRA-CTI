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
public class ServerErrorBurstRule implements DetectionRule {

    @Override
    public String id() {
        return "ETRA-5XX-001";
    }

    @Override
    public Optional<Detection> evaluate(SecurityEvent event) {
        if (event.getStatusCode() == null || event.getStatusCode() < 500 || event.getStatusCode() > 599) {
            return Optional.empty();
        }

        Detection d = new Detection();
        d.setEventId(event.getId());
        d.setRuleId(id());
        d.setType("UNEXPECTED_SERVER_ERROR");
        d.setSeverity(Severity.MEDIUM);
        d.setConfidence(new BigDecimal("0.750"));
        d.setDetails(Map.of(
            "ruleCode", id(),
            "title", "Unexpected server error",
            "statusCode", event.getStatusCode(),
            "endpoint", String.valueOf(event.getPath()),
            "sourceIp", String.valueOf(event.getSourceIp()),
            "reason", "Server returned HTTP " + event.getStatusCode() + " response indicating potential unhandled exception or crash condition"
        ));

        return Optional.of(d);
    }
}
