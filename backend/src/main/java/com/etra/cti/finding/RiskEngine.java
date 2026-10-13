package com.etra.cti.finding;

import com.etra.cti.application.Application;
import com.etra.cti.detection.Severity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class RiskEngine {

    public record RiskResult(BigDecimal score, String band) {}

    /**
     * Risk = severityWeight * confidence * criticalityWeight * exposureWeight * attackContext
     * Then scaled to 0-100.
     */
    public RiskResult calculate(Severity severity,
                                BigDecimal confidence,
                                Application app,
                                double attackContext) {
        double sev   = severityWeight(severity);
        double conf  = confidence != null ? confidence.doubleValue() : 0.8;
        double crit  = criticalityWeight(app.getCriticality().name());
        double expo  = exposureWeight(app.getEnvironment().name());
        double ctx   = Math.min(Math.max(attackContext, 1.0), 2.0);

        double raw = sev * conf * crit * expo * ctx;
        double normalized = Math.min(100.0, raw * 4.0);

        return new RiskResult(
            BigDecimal.valueOf(normalized).setScale(3, RoundingMode.HALF_UP),
            band(normalized)
        );
    }

    private double severityWeight(Severity s) {
        return switch (s) {
            case LOW      -> 1.0;
            case MEDIUM   -> 2.5;
            case HIGH     -> 4.0;
            case CRITICAL -> 5.0;
        };
    }

    private double criticalityWeight(String c) {
        return switch (c) {
            case "LOW"      -> 0.8;
            case "MEDIUM"   -> 1.0;
            case "HIGH"     -> 1.3;
            case "CRITICAL" -> 1.6;
            default         -> 1.0;
        };
    }

    private double exposureWeight(String env) {
        return switch (env) {
            case "DEV"        -> 0.5;
            case "STAGING"    -> 0.8;
            case "PRODUCTION" -> 1.5;
            default           -> 1.0;
        };
    }

    private String band(double v) {
        if (v <= 20) return "LOW";
        if (v <= 40) return "MEDIUM";
        if (v <= 70) return "HIGH";
        return "CRITICAL";
    }
}
