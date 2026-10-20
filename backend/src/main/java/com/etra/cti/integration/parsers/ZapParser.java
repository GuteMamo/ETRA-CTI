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
        if (json == null) {
            return List.of();
        }
        JsonNode root;
        try {
            root = mapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid ZAP JSON report: " + e.getMessage(), e);
        }
        if (root == null || root.isMissingNode() || root.isNull()) {
            return List.of();
        }

        List<ImportedFinding> out = new ArrayList<>();

        // ZAP standard JSON format: root.site[].alerts[]
        JsonNode sites = root.path("site");
        if (sites.isArray()) {
            for (JsonNode site : sites) {
                String siteName = sanitize(site.path("@name").asText(""));
                JsonNode alerts = site.path("alerts");
                if (alerts.isArray()) {
                    for (JsonNode alert : alerts) {
                        out.add(fromAlert(alert, siteName));
                    }
                }
            }
        } else if (root.path("alerts").isArray()) {
            // Alternative flat format: root.alerts[]
            for (JsonNode alert : root.path("alerts")) {
                out.add(fromAlert(alert, ""));
            }
        }

        return out;
    }

    private ImportedFinding fromAlert(JsonNode alert, String site) {
        String name = sanitize(alert.path("alert").asText(alert.path("name").asText("ZAP Finding")));
        String risk = alert.path("riskdesc").asText(alert.path("riskcode").asText("Medium"));
        String desc = sanitize(alert.path("desc").asText(""));
        String solution = sanitize(alert.path("solution").asText(""));

        String endpoint = site;
        JsonNode instances = alert.path("instances");
        if (instances.isArray() && instances.size() > 0) {
            JsonNode first = instances.get(0);
            String uri = sanitize(first.path("uri").asText(""));
            if (!uri.isBlank()) endpoint = uri;
        }

        String param = "";
        if (instances.isArray() && instances.size() > 0) {
            param = sanitize(instances.get(0).path("param").asText(""));
        }

        String finalDesc = desc.isBlank() ? (solution.isBlank() ? name : solution) : desc;

        return new ImportedFinding(
            name,
            finalDesc,
            endpoint.isBlank() ? "N/A" : endpoint,
            param.isBlank() ? null : param,
            mapSeverity(risk),
            solution,
            "ZAP"
        );
    }

    private Severity mapSeverity(String s) {
        if (s == null) return Severity.MEDIUM;
        String lower = s.toLowerCase();
        if (lower.contains("critical")) return Severity.CRITICAL;
        if (lower.contains("high")) return Severity.HIGH;
        if (lower.contains("medium")) return Severity.MEDIUM;
        if (lower.contains("low") || lower.contains("informational")) return Severity.LOW;
        return Severity.MEDIUM;
    }

    private String sanitize(String text) {
        if (text == null) return "";
        // Strip null bytes and non-printable control characters (except newline and tab)
        return text.replaceAll("[\u0000-\u0008\u000B\u000C\u000E-\u001F]", "").trim();
    }
}
