package com.etra.cti.ai;

import com.etra.cti.ai.dto.AiResultDto;
import com.etra.cti.campaign.AttackCampaign;
import com.etra.cti.campaign.AttackCampaignRepository;
import com.etra.cti.campaign.CampaignEventRepository;
import com.etra.cti.common.exception.NotFoundException;
import com.etra.cti.evidence.EvidenceRepository;
import com.etra.cti.finding.Finding;
import com.etra.cti.finding.FindingRepository;
import com.etra.cti.investigation.Investigation;
import com.etra.cti.investigation.InvestigationNoteRepository;
import com.etra.cti.investigation.InvestigationRepository;
import com.etra.cti.remediation.Remediation;
import com.etra.cti.remediation.RemediationRepository;
import com.etra.cti.remediation.RetestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final AiAnalysisRepository cache;
    private final InvestigationRepository investigations;
    private final InvestigationNoteRepository notes;
    private final AttackCampaignRepository campaigns;
    private final CampaignEventRepository campaignEvents;
    private final FindingRepository findings;
    private final RemediationRepository remediations;
    private final RetestRepository retests;
    private final EvidenceRepository evidence;

    @Value("${etra.ai.api-key:}")
    private String apiKey;

    @Value("${etra.ai.model:gpt-4o-mini}")
    private String model;

    public AiService(AiAnalysisRepository cache,
                     InvestigationRepository investigations,
                     InvestigationNoteRepository notes,
                     AttackCampaignRepository campaigns,
                     CampaignEventRepository campaignEvents,
                     FindingRepository findings,
                     RemediationRepository remediations,
                     RetestRepository retests,
                     EvidenceRepository evidence) {
        this.cache = cache;
        this.investigations = investigations;
        this.notes = notes;
        this.campaigns = campaigns;
        this.campaignEvents = campaignEvents;
        this.findings = findings;
        this.remediations = remediations;
        this.retests = retests;
        this.evidence = evidence;
    }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    // ---------- Investigation summary ----------

    @Transactional
    public AiResultDto summarizeInvestigation(UUID investigationId) {
        Investigation inv = investigations.findById(investigationId)
            .orElseThrow(() -> new NotFoundException("Investigation not found"));

        String prompt = buildInvestigationPrompt(inv);
        return runAnalysis("INVESTIGATION_SUMMARY", "Investigation", investigationId.toString(), prompt);
    }

    // ---------- Remediation recommendation ----------

    @Transactional
    public AiResultDto recommendRemediation(UUID findingId) {
        Finding f = findings.findById(findingId)
            .orElseThrow(() -> new NotFoundException("Finding not found"));

        String prompt = buildFindingPrompt(f);
        return runAnalysis("REMEDIATION_RECOMMENDATION", "Finding", findingId.toString(), prompt);
    }

    // ---------- Internals ----------

    private AiResultDto runAnalysis(String type, String resourceType, String resourceId, String prompt) {
        String hash = sha256(prompt);

        // Cache lookup - return immediately if we've seen this exact prompt
        var cached = cache.findByAnalysisTypeAndResourceTypeAndResourceIdAndPromptHash(
            type, resourceType, resourceId, hash);
        if (cached.isPresent()) {
            return new AiResultDto(type, resourceType, resourceId,
                cached.get().getResultText(), cached.get().getModel(), true);
        }

        if (!isEnabled()) {
            String stub = "(AI disabled ? set etra.ai.api-key to enable) " + heuristicFallback(type, prompt);
            return new AiResultDto(type, resourceType, resourceId, stub, "stub", false);
        }

        String result = callOpenAi(prompt);

        AiAnalysis a = new AiAnalysis();
        a.setAnalysisType(type);
        a.setResourceType(resourceType);
        a.setResourceId(resourceId);
        a.setPromptHash(hash);
        a.setResultText(result);
        a.setModel(model);
        cache.save(a);

        return new AiResultDto(type, resourceType, resourceId, result, model, false);
    }

    private String callOpenAi(String prompt) {
        try {
            RestClient client = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();

            Map<String, Object> body = Map.of(
                "model", model,
                "messages", new Object[]{
                    Map.of("role", "system", "content",
                        "You are a senior application security analyst. Be concise, technical, and actionable."),
                    Map.of("role", "user", "content", prompt)
                },
                "temperature", 0.2
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = client.post()
                .uri("/chat/completions")
                .body(body)
                .retrieve()
                .body(Map.class);

            if (response == null) return "(no response from AI provider)";
            var choices = (java.util.List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) return "(no choices returned)";
            var message = (Map<String, Object>) choices.get(0).get("message");
            return String.valueOf(message.get("content"));
        } catch (Exception e) {
            log.error("AI call failed", e);
            return "(AI call failed: " + e.getMessage() + ")";
        }
    }

    private String buildInvestigationPrompt(Investigation inv) {
        StringBuilder sb = new StringBuilder();
        sb.append("Investigation ").append(inv.getReference())
          .append(" ? title: ").append(inv.getTitle()).append("\n");
        sb.append("Status: ").append(inv.getStatus()).append(", Priority: ").append(inv.getPriority()).append("\n");
        sb.append("Description: ").append(inv.getDescription()).append("\n\n");

        if (inv.getCampaignId() != null) {
            AttackCampaign c = campaigns.findById(inv.getCampaignId()).orElse(null);
            if (c != null) {
                sb.append("Linked campaign: ").append(c.getReference())
                  .append(" from source IP ").append(c.getSourceIp())
                  .append(", events=").append(c.getEventCount())
                  .append(", risk=").append(c.getRiskBand()).append("\n");
                sb.append("Timeline:\n");
                campaignEvents.findByCampaignIdOrderBySequenceNoAsc(c.getId()).forEach(e ->
                    sb.append("  ").append(e.getSequenceNo()).append(". ")
                      .append(e.getStage()).append(" ? ").append(e.getDetectionType()).append("\n"));
            }
        }

        sb.append("\nAnalyst notes:\n");
        notes.findByInvestigationIdOrderByCreatedAtAsc(inv.getId()).forEach(n ->
            sb.append("  - ").append(n.getNote()).append("\n"));

        sb.append("\nWrite a 4-6 sentence summary of this investigation for a security manager.\n");
        return sb.toString();
    }

    private String buildFindingPrompt(Finding f) {
        StringBuilder sb = new StringBuilder();
        sb.append("Finding ").append(f.getReference()).append(" ? ").append(f.getTitle()).append("\n");
        sb.append("Severity: ").append(f.getSeverity()).append(", riskBand=").append(f.getRiskBand()).append("\n");
        sb.append("Endpoint: ").append(f.getEndpoint()).append(", parameter: ").append(f.getParameter()).append("\n");
        sb.append("Description: ").append(f.getDescription()).append("\n");

        Remediation r = remediations.findByFindingId(f.getId()).orElse(null);
        if (r != null) {
            sb.append("Remediation status: ").append(r.getStatus())
              .append(", reopened=").append(r.getReopenedCount()).append("\n");
            sb.append("Fix notes: ").append(r.getFixNotes()).append("\n");
        }
        sb.append("Evidence artifacts: ").append(evidence.findByFindingId(f.getId()).size()).append("\n");
        sb.append("Retests: ").append(retests.findByFindingIdOrderByCreatedAtDesc(f.getId()).size()).append("\n\n");

        sb.append("Provide 3-5 concrete, ordered remediation steps a developer should take.\n");
        return sb.toString();
    }

    private String heuristicFallback(String type, String prompt) {
        return switch (type) {
            case "INVESTIGATION_SUMMARY" ->
                "Multi-stage activity detected. Review the campaign timeline, " +
                "validate the source IP against threat intel, and confirm containment.";
            case "REMEDIATION_RECOMMENDATION" ->
                "1. Reproduce the issue with the exact evidence payload. " +
                "2. Apply the standard fix for this vulnerability class. " +
                "3. Add a regression test. 4. Retest before closing.";
            default -> "No heuristic available.";
        };
    }

    private String sha256(String s) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
