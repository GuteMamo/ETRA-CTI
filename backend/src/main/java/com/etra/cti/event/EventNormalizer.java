package com.etra.cti.event;

import com.etra.cti.event.dto.IngestEventRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class EventNormalizer {

    private static final Pattern SECRET_PATTERN = Pattern.compile(
        "(?i)(password|passwd|pwd|secret|token|api_?key|auth|bearer)\\s*[:=]\\s*[^&\\s]+|Bearer\\s+[A-Za-z0-9._~+/-]+=*"
    );

    public static Map<String, Object> normalize(IngestEventRequest req) {
        Map<String, Object> n = new HashMap<>();
        n.put("source", req.source() != null ? req.source().name() : "APPLICATION_LOG");
        n.put("method", req.method());
        n.put("path", redact(req.path()));
        n.put("statusCode", req.statusCode());
        n.put("userId", req.userId());

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
                } else if (v instanceof String s) {
                    n.put("meta." + k, redact(s));
                } else {
                    n.put("meta." + k, v);
                }
            });
        }
        return n;
    }

    private static boolean isSensitiveKey(String k) {
        if (k == null) return false;
        String lk = k.toLowerCase();
        return lk.contains("password") || lk.contains("secret") || lk.contains("token")
            || lk.contains("authorization") || lk.contains("cookie") || lk.contains("apikey");
    }

    public static String redact(String input) {
        if (input == null) return null;
        return SECRET_PATTERN.matcher(input).replaceAll("$1=[REDACTED]");
    }
}
