package com.etra.cti.event;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum EventSource {
    APPLICATION_LOG,
    API_GATEWAY,
    BURP,
    ZAP,
    NMAP,
    MANUAL,
    OTHER;

    @JsonCreator
    public static EventSource fromString(String val) {
        if (val == null || val.isBlank()) {
            return APPLICATION_LOG;
        }
        String clean = val.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        for (EventSource s : values()) {
            if (s.name().equalsIgnoreCase(clean)) {
                return s;
            }
        }
        if (clean.contains("GATEWAY") || clean.contains("API") || clean.contains("PROXY")) {
            return API_GATEWAY;
        }
        if (clean.contains("BURP")) return BURP;
        if (clean.contains("ZAP")) return ZAP;
        if (clean.contains("NMAP")) return NMAP;
        if (clean.contains("MANUAL")) return MANUAL;
        return APPLICATION_LOG;
    }
}
