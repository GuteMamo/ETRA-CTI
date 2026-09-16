package com.etra.cti.common.error;

import java.time.Instant;
import java.util.Map;

public record ApiError(
    String code,
    String message,
    Map<String, String> details,
    Instant timestamp
) {
    public ApiError(String code, String message) {
        this(code, message, null, Instant.now());
    }
    public ApiError(String code, String message, Map<String, String> details) {
        this(code, message, details, Instant.now());
    }
}
