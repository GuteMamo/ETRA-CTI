package com.etra.cti.detection;

import com.etra.cti.detection.rules.*;
import com.etra.cti.event.EventNormalizer;
import com.etra.cti.event.EventSource;
import com.etra.cti.event.SecurityEvent;
import com.etra.cti.event.SecurityEventRepository;
import com.etra.cti.event.dto.IngestEventRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class DetectionRulesTest {

    @Test
    @DisplayName("SqlInjectionRule detects SQLi pattern in path/query")
    void testSqlInjectionRule() {
        SqlInjectionRule rule = new SqlInjectionRule();
        SecurityEvent event = new SecurityEvent();
        event.setId(UUID.randomUUID());
        event.setPath("/dvwa/vulnerabilities/sqli/?id=1' OR '1'='1");
        event.setNormalized(EventNormalizer.normalize(new IngestEventRequest(
            UUID.randomUUID(), EventSource.APPLICATION_LOG, Instant.now(),
            "127.0.0.1", "GET", event.getPath(), 200, "user1", Map.of()
        )));

        var detection = rule.evaluate(event);
        assertTrue(detection.isPresent());
        assertEquals("SQL_INJECTION_INDICATOR", detection.get().getType());
        assertEquals(Severity.HIGH, detection.get().getSeverity());
    }

    @Test
    @DisplayName("XssRule detects reflected XSS payload")
    void testXssRule() {
        XssRule rule = new XssRule();
        SecurityEvent event = new SecurityEvent();
        event.setId(UUID.randomUUID());
        event.setPath("/search?q=<script>alert(1)</script>");
        event.setNormalized(EventNormalizer.normalize(new IngestEventRequest(
            UUID.randomUUID(), EventSource.APPLICATION_LOG, Instant.now(),
            "127.0.0.1", "GET", event.getPath(), 200, "user1", Map.of()
        )));

        var detection = rule.evaluate(event);
        assertTrue(detection.isPresent());
        assertEquals("XSS_INDICATOR", detection.get().getType());
        assertEquals(Severity.HIGH, detection.get().getSeverity());
    }

    @Test
    @DisplayName("PathTraversalRule detects directory traversal attempt")
    void testPathTraversalRule() {
        PathTraversalRule rule = new PathTraversalRule();
        SecurityEvent event = new SecurityEvent();
        event.setId(UUID.randomUUID());
        event.setPath("/download?file=../../etc/passwd");
        event.setNormalized(EventNormalizer.normalize(new IngestEventRequest(
            UUID.randomUUID(), EventSource.APPLICATION_LOG, Instant.now(),
            "127.0.0.1", "GET", event.getPath(), 200, "user1", Map.of()
        )));

        var detection = rule.evaluate(event);
        assertTrue(detection.isPresent());
        assertEquals("PATH_TRAVERSAL_INDICATOR", detection.get().getType());
    }

    @Test
    @DisplayName("SuspiciousApiProbingRule detects probing of sensitive endpoints")
    void testSuspiciousApiProbingRule() {
        SuspiciousApiProbingRule rule = new SuspiciousApiProbingRule();
        SecurityEvent event = new SecurityEvent();
        event.setId(UUID.randomUUID());
        event.setPath("/.env");
        event.setNormalized(Map.of("endpoint", "/.env"));

        var detection = rule.evaluate(event);
        assertTrue(detection.isPresent());
        assertEquals("SUSPICIOUS_API_PROBING", detection.get().getType());
    }

    @Test
    @DisplayName("ServerErrorBurstRule detects 5xx server errors")
    void testServerErrorBurstRule() {
        ServerErrorBurstRule rule = new ServerErrorBurstRule();
        SecurityEvent event = new SecurityEvent();
        event.setId(UUID.randomUUID());
        event.setStatusCode(500);
        event.setPath("/api/orders/checkout");

        var detection = rule.evaluate(event);
        assertTrue(detection.isPresent());
        assertEquals("UNEXPECTED_SERVER_ERROR", detection.get().getType());
    }

    @Test
    @DisplayName("AuthFailureBurstRule triggers when failed login threshold in window is met")
    void testAuthFailureBurstRule() {
        SecurityEventRepository mockRepo = Mockito.mock(SecurityEventRepository.class);
        AuthFailureBurstRule rule = new AuthFailureBurstRule(mockRepo);

        UUID appId = UUID.randomUUID();
        SecurityEvent currentEvent = new SecurityEvent();
        currentEvent.setId(UUID.randomUUID());
        currentEvent.setApplicationId(appId);
        currentEvent.setSourceIp("192.168.1.100");
        currentEvent.setPath("/api/auth/login");
        currentEvent.setStatusCode(401);
        currentEvent.setEventTime(Instant.now());

        SecurityEvent e1 = new SecurityEvent();
        e1.setId(UUID.randomUUID());
        e1.setStatusCode(401);

        SecurityEvent e2 = new SecurityEvent();
        e2.setId(UUID.randomUUID());
        e2.setStatusCode(401);

        SecurityEvent e3 = new SecurityEvent();
        e3.setId(UUID.randomUUID());
        e3.setStatusCode(401);

        when(mockRepo.findByApplicationIdAndSourceIpAndEventTimeAfter(eq(appId), eq("192.168.1.100"), any()))
            .thenReturn(List.of(e1, e2, e3));

        var detection = rule.evaluate(currentEvent);
        assertTrue(detection.isPresent());
        assertEquals("AUTH_FAILURE_BURST", detection.get().getType());
        assertEquals(Severity.HIGH, detection.get().getSeverity());
    }

    @Test
    @DisplayName("PathTraversalRule and XssRule safely handle null normalized map and null event")
    void testRulesHandleNullNormalizedMapGracefully() {
        PathTraversalRule ptRule = new PathTraversalRule();
        XssRule xssRule = new XssRule();

        SecurityEvent nullEvent = null;
        assertFalse(ptRule.evaluate(nullEvent).isPresent());
        assertFalse(xssRule.evaluate(nullEvent).isPresent());

        SecurityEvent eventWithNullNormalized = new SecurityEvent();
        eventWithNullNormalized.setId(UUID.randomUUID());
        eventWithNullNormalized.setPath(null);
        eventWithNullNormalized.setNormalized(null);

        assertDoesNotThrow(() -> assertFalse(ptRule.evaluate(eventWithNullNormalized).isPresent()));
        assertDoesNotThrow(() -> assertFalse(xssRule.evaluate(eventWithNullNormalized).isPresent()));
    }
}
