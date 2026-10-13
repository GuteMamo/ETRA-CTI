package com.etra.cti.remediation.kb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Matches a finding title/description against the remediation knowledge base
 * and returns the best-fit recommendation.
 */
@Service
public class RemediationAdvisor {

    private static final Logger log = LoggerFactory.getLogger(RemediationAdvisor.class);

    private final RemediationKnowledgeRepository kb;

    public RemediationAdvisor(RemediationKnowledgeRepository kb) {
        this.kb = kb;
    }

    public record Recommendation(
        String title,
        String recommendation,
        String category,
        String cwe,
        String owasp,
        String priority
    ) {}

    public Recommendation advise(String title, String description) {
        String hay = ((title == null ? "" : title) + " " + (description == null ? "" : description))
            .toLowerCase();

        // Longest keyword match wins (most specific)
        for (RemediationKnowledge k : kb.findAllOrderedByKeywordLength()) {
            if ("default".equalsIgnoreCase(k.getMatchKeyword())) continue;
            if (hay.contains(k.getMatchKeyword().toLowerCase())) {
                log.info(">>> KB match: '{}' ? {} ({})",
                    k.getMatchKeyword(), k.getTitle(), k.getCategory());
                return new Recommendation(
                    k.getTitle(), k.getRecommendation(), k.getCategory(),
                    k.getCwe(), k.getOwasp(), k.getPriority()
                );
            }
        }

        // Fall back to "default"
        Optional<RemediationKnowledge> fallback = kb.findAllOrderedByKeywordLength().stream()
            .filter(k -> "default".equalsIgnoreCase(k.getMatchKeyword()))
            .findFirst();

        if (fallback.isPresent()) {
            RemediationKnowledge k = fallback.get();
            return new Recommendation(
                "Generic remediation needed",
                k.getRecommendation(),
                k.getCategory(),
                null, null, k.getPriority()
            );
        }

        return new Recommendation(
            "Generic remediation needed",
            "Review the finding, reproduce the issue, apply the standard fix, and add a regression test.",
            "GENERIC", null, null, "MEDIUM"
        );
    }
}
