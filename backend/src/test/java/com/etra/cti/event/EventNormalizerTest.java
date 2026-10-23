package com.etra.cti.event;

import com.etra.cti.detection.rules.SqlInjectionRule;
import com.etra.cti.event.EventSource;
import com.etra.cti.event.dto.IngestEventRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class EventNormalizerTest {

    @Test
    @DisplayName("Case-insensitive sensitive key detection and redaction for all standard aliases")
    void testSensitiveKeyAliasesRedacted() {
        String[] aliases = {
            "password", "Password", "PASSWORD", "user_password", "new-password",
            "authorization", "Authorization", "AUTHORIZATION", "proxy-authorization",
            "cookie", "Cookie", "COOKIE", "set-cookie",
            "access_token", "accessToken", "ACCESS_TOKEN", "access-token",
            "refresh_token", "refreshToken", "REFRESH_TOKEN", "refresh-token",
            "api_key", "apiKey", "API_KEY", "api-key", "apikey",
            "client_secret", "clientSecret", "CLIENT_SECRET", "client-secret",
            "passwd", "pwd", "credential", "private_key", "session_id"
        };

        for (String alias : aliases) {
            Map<String, Object> meta = new HashMap<>();
            meta.put(alias, "synth_secret_val_123");
            meta.put("public_field", "safe_val");

            Map<String, Object> redacted = EventNormalizer.redactPayload(meta);
            assertNotNull(redacted, "Failed for alias: " + alias);
            assertEquals("[REDACTED]", redacted.get(alias), "Alias was not redacted: " + alias);
            assertEquals("safe_val", redacted.get("public_field"));
        }
    }

    @Test
    @DisplayName("Nested objects and arrays are recursively redacted")
    void testNestedObjectsAndCollectionsRedaction() {
        Map<String, Object> childMap = new HashMap<>();
        childMap.put("client_secret", "synth_child_secret_xyz");
        childMap.put("username", "security_officer");

        List<Object> listWithSecrets = new ArrayList<>();
        listWithSecrets.add("safe_item");
        Map<String, Object> listMap = new HashMap<>();
        listMap.put("api_key", "synth_list_key_999");
        listWithSecrets.add(listMap);

        Map<String, Object> meta = new HashMap<>();
        meta.put("user_context", childMap);
        meta.put("records_list", listWithSecrets);
        meta.put("token_list", List.of("synth_tok_a", "synth_tok_b"));
        meta.put("server", "nginx");

        Map<String, Object> redacted = EventNormalizer.redactPayload(meta);
        assertNotNull(redacted);

        // Child map checks
        @SuppressWarnings("unchecked")
        Map<String, Object> redChild = (Map<String, Object>) redacted.get("user_context");
        assertEquals("[REDACTED]", redChild.get("client_secret"));
        assertEquals("security_officer", redChild.get("username"));

        // Non-sensitive list key with nested sensitive child map
        @SuppressWarnings("unchecked")
        List<Object> redList = (List<Object>) redacted.get("records_list");
        assertEquals("safe_item", redList.get(0));
        @SuppressWarnings("unchecked")
        Map<String, Object> redListMap = (Map<String, Object>) redList.get(1);
        assertEquals("[REDACTED]", redListMap.get("api_key"));

        // Sensitive list key is directly redacted to string
        assertEquals("[REDACTED]", redacted.get("token_list"));

        assertEquals("nginx", redacted.get("server"));
    }

    @Test
    @DisplayName("URL path and query parameter secrets are redacted while keeping endpoint structure")
    void testPathAndQueryStringRedaction() {
        String rawPath = "/api/v1/auth/callback?access_token=synth_access_abc123&client_secret=synth_sec_999&code=valid_code";
        String redactedPath = EventNormalizer.redact(rawPath);

        assertFalse(redactedPath.contains("synth_access_abc123"), "access_token was not redacted from path");
        assertFalse(redactedPath.contains("synth_sec_999"), "client_secret was not redacted from path");
        assertTrue(redactedPath.contains("access_token=[REDACTED]"));
        assertTrue(redactedPath.contains("client_secret=[REDACTED]"));
        assertTrue(redactedPath.contains("code=valid_code"));
    }

    @Test
    @DisplayName("Authorization header Bearer tokens are redacted in strings and headers")
    void testBearerTokenRedaction() {
        String authHeader = "Bearer synth_jwt_token_payload_with_entropy_999";
        String redacted = EventNormalizer.redact(authHeader);

        assertEquals("Bearer [REDACTED]", redacted);
        assertFalse(redacted.contains("synth_jwt_token"));
    }

    @Test
    @DisplayName("Event normalization redacts path, query, and metadata before persistence")
    void testNormalizeRedactsAllSurfaces() {
        Map<String, Object> meta = Map.of(
            "password", "synth_user_password_777",
            "safe_header", "text/plain"
        );
        IngestEventRequest req = new IngestEventRequest(
            UUID.randomUUID(),
            EventSource.API_GATEWAY,
            Instant.now(),
            "192.168.1.100",
            "POST",
            "/login?session_id=synth_sess_111",
            200,
            "user-42",
            meta
        );

        Map<String, Object> norm = EventNormalizer.normalize(req);
        assertNotNull(norm);
        assertEquals("[REDACTED]", norm.get("meta.password"));
        assertEquals("text/plain", norm.get("meta.safe_header"));
        assertTrue(String.valueOf(norm.get("path")).contains("session_id=[REDACTED]"));
        assertTrue(String.valueOf(norm.get("query")).contains("session_id=[REDACTED]"));
        assertEquals("/login", norm.get("endpoint"));
    }

    @Test
    @DisplayName("Detection rules still detect attacks when query contains both attack pattern and secret")
    void testDetectionRulesStillOperateWithRedactedPayload() {
        // Query has both a sensitive key and an SQL injection payload
        String rawPath = "/search?password=synth_sec_123&q=UNION+SELECT+1,2";
        String redactedPath = EventNormalizer.redact(rawPath);

        // Verify password is redacted but SQL injection payload remains
        assertFalse(redactedPath.contains("synth_sec_123"));
        assertTrue(redactedPath.contains("UNION SELECT 1,2") || redactedPath.contains("UNION+SELECT+1,2"));

        SecurityEvent event = new SecurityEvent();
        event.setId(UUID.randomUUID());
        event.setPath(redactedPath);
        event.setNormalized(Map.of(
            "endpoint", "/search",
            "query", "password=[REDACTED]&q=UNION SELECT 1,2"
        ));

        SqlInjectionRule rule = new SqlInjectionRule();
        var detection = rule.evaluate(event);
        assertTrue(detection.isPresent(), "SQL injection rule must detect pattern even when secrets are redacted");
        assertEquals("SQL_INJECTION_INDICATOR", detection.get().getType());
    }
}
