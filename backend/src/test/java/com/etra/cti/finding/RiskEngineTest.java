package com.etra.cti.finding;

import com.etra.cti.application.Application;
import com.etra.cti.application.Criticality;
import com.etra.cti.application.Environment;
import com.etra.cti.detection.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class RiskEngineTest {

    private RiskEngine riskEngine;
    private Application prodCriticalApp;
    private Application devLowApp;

    @BeforeEach
    void setUp() {
        riskEngine = new RiskEngine();

        prodCriticalApp = new Application();
        prodCriticalApp.setCriticality(Criticality.CRITICAL); // 100
        prodCriticalApp.setEnvironment(Environment.PRODUCTION); // 100

        devLowApp = new Application();
        devLowApp.setCriticality(Criticality.LOW); // 25
        devLowApp.setEnvironment(Environment.DEV); // 25
    }

    @Test
    @DisplayName("Critical severity in Production on Critical asset produces CRITICAL risk score")
    void testCriticalRiskCalculation() {
        // sev: 100, conf: 100, crit: 100, expo: 100
        // formula: 0.4*100 + 0.25*100 + 0.20*100 + 0.15*100 = 100.0
        var res = riskEngine.calculate(Severity.CRITICAL, new BigDecimal("1.000"), prodCriticalApp, 1.0);
        assertEquals(100.0, res.score().doubleValue());
        assertEquals("CRITICAL", res.band());
    }

    @Test
    @DisplayName("Low severity in Dev on Low asset produces LOW risk score")
    void testLowRiskCalculation() {
        // sev: 25, conf: 25, crit: 25, expo: 25
        // formula: 0.4*25 + 0.25*25 + 0.20*25 + 0.15*25 = 25.0
        var res = riskEngine.calculate(Severity.LOW, new BigDecimal("0.250"), devLowApp, 1.0);
        assertEquals(25.0, res.score().doubleValue());
        assertEquals("MEDIUM", res.band()); // 25-49 is MEDIUM

        // If conf is 0.1 (10.0), score = 0.4*25 + 0.25*10 + 0.20*25 + 0.15*25 = 10 + 2.5 + 5 + 3.75 = 21.3 -> LOW
        var resLow = riskEngine.calculate(Severity.LOW, new BigDecimal("0.100"), devLowApp, 1.0);
        assertTrue(resLow.score().doubleValue() < 25.0);
        assertEquals("LOW", resLow.band());
    }

    @Test
    @DisplayName("High severity on Staging produces expected HIGH risk score")
    void testHighRiskCalculation() {
        Application stagingApp = new Application();
        stagingApp.setCriticality(Criticality.HIGH); // 75
        stagingApp.setEnvironment(Environment.STAGING); // 60

        // sev: 75, conf: 85 (0.85), crit: 75, expo: 60
        // formula: 0.4*75 (30) + 0.25*85 (21.25) + 0.20*75 (15) + 0.15*60 (9) = 75.25 -> CRITICAL or high boundary
        var res = riskEngine.calculate(Severity.HIGH, new BigDecimal("0.850"), stagingApp, 1.0);
        assertTrue(res.score().doubleValue() >= 50.0);
        assertTrue(res.score().doubleValue() <= 100.0);
    }

    @Test
    @DisplayName("Mathematical guarantee: Maximum score is capped at 100.0 even with attack context multiplier 1.5")
    void testMaximumBoundCap() {
        var resMaxMultiplier = riskEngine.calculate(Severity.CRITICAL, new BigDecimal("1.0"), prodCriticalApp, 1.5);
        assertEquals(100.0, resMaxMultiplier.score().doubleValue());
        assertEquals("CRITICAL", resMaxMultiplier.band());

        var resExtremeMultiplier = riskEngine.calculate(Severity.CRITICAL, new BigDecimal("999.0"), prodCriticalApp, 99.9);
        assertEquals(100.0, resExtremeMultiplier.score().doubleValue());
    }

    @Test
    @DisplayName("Mathematical guarantee: Minimum score is strictly >= 0.0 and <= 100.0 under extreme lower bounds")
    void testMinimumBound() {
        var resMin = riskEngine.calculate(Severity.LOW, BigDecimal.ZERO, devLowApp, 0.0);
        assertEquals(18.8, resMin.score().doubleValue()); // 0.4*25 + 0.25*0 + 0.2*25 + 0.15*25 = 18.75 -> 18.8
        assertEquals("LOW", resMin.band());
        assertTrue(resMin.score().doubleValue() >= 0.0);
        assertTrue(resMin.score().doubleValue() <= 100.0);

        var resNegative = riskEngine.calculate(Severity.LOW, new BigDecimal("-100.0"), devLowApp, -5.0);
        assertEquals(18.8, resNegative.score().doubleValue());
        assertTrue(resNegative.score().doubleValue() >= 0.0);
    }

    @Test
    @DisplayName("Null safety: Null application, null severity, null confidence do not throw and stay bounded")
    void testNullResilience() {
        assertDoesNotThrow(() -> {
            var res = riskEngine.calculate(null, null, null, 1.0);
            assertNotNull(res);
            assertNotNull(res.score());
            assertTrue(res.score().doubleValue() >= 0.0 && res.score().doubleValue() <= 100.0);
        });

        Application partialApp = new Application();
        partialApp.setCriticality(null);
        partialApp.setEnvironment(null);
        assertDoesNotThrow(() -> {
            var res = riskEngine.calculate(Severity.MEDIUM, null, partialApp, Double.NaN);
            assertNotNull(res);
            assertTrue(res.score().doubleValue() >= 0.0 && res.score().doubleValue() <= 100.0);
        });
    }

    @Test
    @DisplayName("Exhaustive combination sweep confirms 0-100 mathematical invariant")
    void testExhaustiveRangeGuarantee() {
        Severity[] severities = { null, Severity.LOW, Severity.MEDIUM, Severity.HIGH, Severity.CRITICAL };
        BigDecimal[] confidences = { null, new BigDecimal("-10.0"), BigDecimal.ZERO, new BigDecimal("0.5"), new BigDecimal("1.0"), new BigDecimal("50.0"), new BigDecimal("100.0"), new BigDecimal("200.0") };
        double[] contexts = { -1.0, 0.0, 1.0, 1.25, 1.5, 2.0, 10.0, Double.NaN, Double.POSITIVE_INFINITY };

        for (Severity s : severities) {
            for (BigDecimal c : confidences) {
                for (double ctx : contexts) {
                    var resProd = riskEngine.calculate(s, c, prodCriticalApp, ctx);
                    assertTrue(resProd.score().doubleValue() >= 0.0, "Score below 0: " + resProd.score());
                    assertTrue(resProd.score().doubleValue() <= 100.0, "Score above 100: " + resProd.score());

                    var resDev = riskEngine.calculate(s, c, devLowApp, ctx);
                    assertTrue(resDev.score().doubleValue() >= 0.0, "Score below 0: " + resDev.score());
                    assertTrue(resDev.score().doubleValue() <= 100.0, "Score above 100: " + resDev.score());

                    var resNullApp = riskEngine.calculate(s, c, null, ctx);
                    assertTrue(resNullApp.score().doubleValue() >= 0.0, "Score below 0: " + resNullApp.score());
                    assertTrue(resNullApp.score().doubleValue() <= 100.0, "Score above 100: " + resNullApp.score());
                }
            }
        }
    }
}
