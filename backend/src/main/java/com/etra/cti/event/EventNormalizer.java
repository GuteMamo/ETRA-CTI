package com.etra.cti.event;

import com.etra.cti.event.dto.IngestEventRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class EventNormalizer {

    private static final Pattern SECRET_PATTERN = Pattern.compile(
        "(?i)(password|passwd|pwd|secret|client_?secret|token|access_?token|refresh_?token|api_?key|apikey|auth|authorization|bearer|cookie|credential|session_?id)\\s*[:=]\\s*[^&\\s;,]+|Bearer\\s+[A-Za-z0-9._~+/-]+=*"
    );

    public static Map<String, Object> normalize(IngestEventRequest req) {
        Map<String, Object> n = new HashMap<>();
        n.put("source", req.source() != null ? req.source().name() : "APPLICATION_LOG");
        n.put("method", req.method() != null && !req.method().isBlank() ? req.method().trim().toUpperCase() : "POST");
        n.put("path", redact(req.path() != null && !req.path().isBlank() ? req.path().trim() : "/"));
        n.put("statusCode", req.statusCode() != null ? req.statusCode() : 200);
        n.put("userId", req.userId() != null && !req.userId().isBlank() ? req.userId().trim() : "anonymous");

        if (req.path() != null) {
            int q = req.path().indexOf('?');
            if (q >= 0) {
                n.put("query", redact(req.path().substring(q + 1)));
                n.put("endpoint", req.path().substring(0, q));
            } else {
                n.put("endpoint", req.path());
            }
        }

        if (req.metadata() != null) {
            req.metadata().forEach((k, v) -> {
                if (isSensitiveKey(k)) {
                    n.put("meta." + k, "[REDACTED]");
                } else {
                    n.put("meta." + k, redactValue(v));
                }
            });
        }
        return n;
    }

    public static boolean isSensitiveKey(String k) {
        if (k == null) return false;
        String clean = k.toLowerCase().replaceAll("[_\\-\\s]", "");
        return clean.contains("password")
            || clean.contains("passwd")
            || clean.contains("pwd")
            || clean.contains("secret")
            || clean.contains("token")
            || clean.contains("apikey")
            || clean.contains("authorization")
            || clean.contains("authorisation")
            || clean.contains("cookie")
            || clean.contains("credential")
            || clean.contains("privatekey")
            || clean.contains("sessionid")
            || clean.equals("auth")
            || clean.contains("authtoken")
            || clean.contains("authkey")
            || clean.contains("authheader")
            || clean.contains("authsecret");
    }

    public static String redact(String input) {
        if (input == null) return null;
        return SECRET_PATTERN.matcher(input).replaceAll(match -> {
            String m = match.group();
            if (m.toLowerCase().startsWith("bearer ")) {
                return "Bearer [REDACTED]";
            }
            int eq = m.indexOf('=');
            int col = m.indexOf(':');
            int sep = (eq >= 0 && col >= 0) ? Math.min(eq, col) : Math.max(eq, col);
            if (sep >= 0) {
                return m.substring(0, sep + 1) + "[REDACTED]";
            }
            return "[REDACTED]";
        });
    }

    @SuppressWarnings("unchecked")
    public static Object redactValue(Object val) {
        if (val == null) return null;
        if (val instanceof String s) {
            return redact(s);
        }
        if (val instanceof Map<?, ?> m) {
            return redactPayload((Map<String, Object>) m);
        }
        if (val instanceof java.util.Collection<?> col) {
            java.util.List<Object> list = new java.util.ArrayList<>();
            for (Object item : col) {
                list.add(redactValue(item));
            }
            return list;
        }
        if (val instanceof Object[] arr) {
            Object[] copy = new Object[arr.length];
            for (int i = 0; i < arr.length; i++) {
                copy[i] = redactValue(arr[i]);
            }
            return copy;
        }
        return val;
    }

    public static Map<String, Object> redactPayload(Map<String, Object> metadata) {
        if (metadata == null) return null;
        Map<String, Object> redacted = new HashMap<>();
        metadata.forEach((k, v) -> {
            if (isSensitiveKey(k)) {
                redacted.put(k, "[REDACTED]");
            } else {
                redacted.put(k, redactValue(v));
            }
        });
        return redacted;
    }
}
