package com.etra.cti.event;

import com.etra.cti.event.dto.IngestEventRequest;

import java.util.HashMap;
import java.util.Map;

public class EventNormalizer {

    public static Map<String, Object> normalize(IngestEventRequest req) {
        Map<String, Object> n = new HashMap<>();
        n.put("source", req.source().name());
        n.put("method", req.method());
        n.put("path", req.path());
        n.put("statusCode", req.statusCode());
        n.put("userId", req.userId());

        if (req.path() != null) {
            int q = req.path().indexOf('?');
            if (q >= 0) {
                n.put("query", req.path().substring(q + 1));
                n.put("endpoint", req.path().substring(0, q));
            } else {
                n.put("endpoint", req.path());
            }
        }

        if (req.metadata() != null) {
            req.metadata().forEach((k, v) -> n.put("meta." + k, v));
        }
        return n;
    }
}
