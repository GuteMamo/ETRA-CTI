package com.etra.cti.finding;

import com.etra.cti.application.Application;
import com.etra.cti.detection.Severity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Section 8.3 Risk Score Implementation:
 * riskScore = round(0.40 × severityScore + 0.25 × confidenceScore + 0.20 × assetCriticalityScore + 0.15 × exposureScore)
 *
 * Each component is normalized to 0–100.
 * Score Labels:
 *  0–24  : LOW
 *  25–49 : MEDIUM
 *  50–74 : HIGH
 *  75–100: CRITICAL
 */
@Service
public class RiskEngine {

    public record RiskBreakdown(
        double severityScore,
        double confidenceScore,
        double assetCriticalityScore,
        double exposureScore,
        BigDecimal finalScore,
        String band
    ) {}

    public record RiskResult(BigDecimal score, String band) {}

    public RiskResult calculate(Severity severity,
                                BigDecimal confidence,
                                Application app,
                                double attackContext) {
        RiskBreakdown breakdown = calculateBreakdown(severity, confidence, app, attackContext);
        return new RiskResult(breakdown.finalScore(), breakdown.band());
    }

    public RiskBreakdown calculateBreakdown(Severity severity,
                                           BigDecimal confidence,
                                           Application app,
                                           double attackContext) {
        double sevScore = severityScore(severity);
        double confScore;
        if (confidence == null) {
            confScore = 80.0;
        } else {
            double cVal = confidence.doubleValue();
            if (cVal <= 0.0) {
                confScore = 0.0;
            } else if (cVal <= 1.0) {
                confScore = cVal * 100.0;
            } else if (cVal >= 100.0) {
                confScore = 100.0;
            } else {
                confScore = cVal;
            }
        }

        double critScore = (app != null && app.getCriticality() != null)
            ? criticalityScore(app.getCriticality().name())
            : 50.0;
        double expoScore = (app != null && app.getEnvironment() != null)
            ? exposureScore(app.getEnvironment().name())
            : 50.0;

        // Documented formula: 0.40*sev + 0.25*conf + 0.20*crit + 0.15*expo
        double base = (0.40 * sevScore) + (0.25 * confScore) + (0.20 * critScore) + (0.15 * expoScore);

        // Active attack context boost if multi-stage attacks exist (bounded between 1.0 and 1.5)
        double ctx = (Double.isNaN(attackContext) || Double.isInfinite(attackContext))
            ? 1.0
            : Math.min(Math.max(attackContext, 1.0), 1.5);
        double scaled = Math.min(100.0, Math.max(0.0, base * ctx));

        double rounded = Math.min(100.0, Math.max(0.0, Math.round(scaled * 10.0) / 10.0));
        BigDecimal finalScore = BigDecimal.valueOf(rounded).setScale(1, RoundingMode.HALF_UP);
        String band = band(finalScore.doubleValue());

        return new RiskBreakdown(sevScore, confScore, critScore, expoScore, finalScore, band);
    }

    private double severityScore(Severity s) {
        if (s == null) return 50.0;
        return switch (s) {
            case LOW      -> 25.0;
            case MEDIUM   -> 50.0;
            case HIGH     -> 75.0;
            case CRITICAL -> 100.0;
        };
    }

    private double criticalityScore(String c) {
        if (c == null) return 50.0;
        return switch (c.toUpperCase()) {
            case "LOW"      -> 25.0;
            case "MEDIUM"   -> 50.0;
            case "HIGH"     -> 75.0;
            case "CRITICAL" -> 100.0;
            default         -> 50.0;
        };
    }

    private double exposureScore(String env) {
        if (env == null) return 50.0;
        return switch (env.toUpperCase()) {
            case "DEV"        -> 25.0;
            case "STAGING"    -> 60.0;
            case "PRODUCTION" -> 100.0;
            default           -> 50.0;
        };
    }

    private String band(double v) {
        if (v < 25.0) return "LOW";
        if (v < 50.0) return "MEDIUM";
        if (v < 75.0) return "HIGH";
        return "CRITICAL";
    }
}
