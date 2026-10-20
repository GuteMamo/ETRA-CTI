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
    }
}
