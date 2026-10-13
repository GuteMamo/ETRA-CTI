package com.etra.cti.mobile.parsers;

import com.etra.cti.detection.Severity;
import com.etra.cti.integration.ImportedFinding;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class MobSFParser {

    private final ObjectMapper mapper = new ObjectMapper();

    public record MobSFReport(
        String packageName,
        String versionName,
        String versionCode,
        String minSdk,
        String targetSdk,
        Boolean signed,
        List<String> permissions,
        List<ImportedFinding> findings
    ) {}

    public MobSFReport parse(InputStream json) {
        JsonNode root = mapper.readTree(json);

        String pkg   = root.path("package_name").asText(null);
        String ver   = root.path("version_name").asText(null);
        String vcode = root.path("version_code").asText(null);
        String minSdk   = root.path("min_sdk").asText(null);
        String targetSdk= root.path("target_sdk").asText(null);
        boolean signed = root.path("signed").asBoolean(false);

        List<String> permissions = new ArrayList<>();
        JsonNode perms = root.path("permissions");
        if (perms.isObject()) {
            perms.properties().forEach(entry -> {
                if (entry.getValue().asBoolean(false)) permissions.add(entry.getKey());
            });
        } else if (perms.isArray()) {
            perms.forEach(p -> permissions.add(p.asText()));
        }

        List<ImportedFinding> findings = new ArrayList<>();

        // Manifest issues
        JsonNode manifest = root.path("manifest_analysis");
        if (manifest.isArray()) {
            for (JsonNode m : manifest) {
                String title = m.path("title").asText("Manifest issue");
                String desc  = m.path("description").asText("");
                String sev   = m.path("severity").asText("medium");
                findings.add(new ImportedFinding(
                    "Mobile Manifest: " + title,
                    desc,
                    pkg,
                    null,
                    mapSeverity(sev),
                    desc,
                    "MOBSF_MANIFEST"
                ));
            }
        }

        // Code issues (from code_analysis.findings)
        JsonNode codeAnalysis = root.path("code_analysis").path("findings");
        if (codeAnalysis.isObject()) {
            codeAnalysis.properties().forEach(entry -> {
                JsonNode finding = entry.getValue();
                String title = finding.path("metadata").path("description").asText(entry.getKey());
                String sev   = finding.path("metadata").path("severity").asText("medium");
                String file  = finding.path("files").path(0).path("file_path").asText("");
                findings.add(new ImportedFinding(
                    "Mobile Code: " + title,
                    "Found in: " + file,
                    pkg,
                    file,
                    mapSeverity(sev),
                    file,
                    "MOBSF_CODE"
                ));
            });
        }

        return new MobSFReport(pkg, ver, vcode, minSdk, targetSdk, signed, permissions, findings);
    }

    private Severity mapSeverity(String s) {
        if (s == null) return Severity.MEDIUM;
        String lower = s.toLowerCase();
        if (lower.contains("high") || lower.contains("critical")) return Severity.HIGH;
        if (lower.contains("medium") || lower.contains("warning")) return Severity.MEDIUM;
        if (lower.contains("low") || lower.contains("info")) return Severity.LOW;
        return Severity.MEDIUM;
    }
}
