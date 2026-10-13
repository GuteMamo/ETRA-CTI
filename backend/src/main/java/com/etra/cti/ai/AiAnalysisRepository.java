package com.etra.cti.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, UUID> {

    Optional<AiAnalysis> findByAnalysisTypeAndResourceTypeAndResourceIdAndPromptHash(
        String analysisType, String resourceType, String resourceId, String promptHash);
}
