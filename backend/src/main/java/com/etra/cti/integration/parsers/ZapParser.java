package com.etra.cti.integration.parsers;

import com.etra.cti.detection.Severity;
import com.etra.cti.integration.ImportedFinding;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class ZapParser {

    private final ObjectMapper mapper = new ObjectMapper();

    public List<ImportedFinding> parse(InputStream json) {
        JsonNode root = mapper.readTree(json);
        List<ImportedFinding> out = new ArrayList<>();

        // ZAP JSON report format: root.site[].alerts[]
        JsonNode sites = root.path("site");
        if (sites.isArray()) {
            for (JsonNode site : sites) {
                String siteName = site.path("@name").asText("");
                JsonNode alerts = site.path("alerts");
                if (alerts.isArray()) {
                    for (JsonNode alert : alerts) {
                        out.add(fromAlert(alert, siteName));
                    }
                }
            }
        }

        // Alternative format: root.alerts (flat list)
        JsonNode flatAlerts = root.path("alerts");
        if (flatAlerts.isArray()) {
            for (JsonNode alert : flatAlerts) {
                out.add(fromAlert(alert, ""));
            }
        }

        return out;
    }

    private ImportedFinding fromAlert(JsonNode alert, String site) {
        String name = alert.path("alert").asText(alert.path("name").asText("ZAP Finding"));
        String risk = alert.path("riskdesc").asText(alert.path("riskcode").asText("Medium"));
        String desc = alert.path("desc").asText("");
        String solution = alert.path("solution").asText("");

        String endpoint = site;
        JsonNode instances = alert.path("instances");
        if (instances.isArray() && instances.size() > 0) {
            JsonNode first = instances.get(0);
            String uri = first.path("uri").asText("");
            if (!uri.isBlank()) endpoint = uri;
        }

        String param = "";
        if (instances.isArray() && instances.size() > 0) {
            param = instances.get(0).path("param").asText("");
        }

        return new ImportedFinding(
            name,
            desc.isBlank() ? solution : desc,
            endpoint,
            param.isBlank() ? null : param,
            mapSeverity(risk),
            solution,
            "ZAP"
        );
    }

    private Severity mapSeverity(String s) {
        if (s == null) return Severity.MEDIUM;
        String lower = s.toLowerCase();
        if (lower.contains("high")) return Severity.HIGH;
        if (lower.contains("medium")) return Severity.MEDIUM;
        if (lower.contains("low")) return Severity.LOW;
        return Severity.MEDIUM;
    }
}
