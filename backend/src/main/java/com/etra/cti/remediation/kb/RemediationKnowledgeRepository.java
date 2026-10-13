package com.etra.cti.remediation.kb;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface RemediationKnowledgeRepository extends JpaRepository<RemediationKnowledge, UUID> {

    @Query("SELECT k FROM RemediationKnowledge k ORDER BY LENGTH(k.matchKeyword) DESC")
    List<RemediationKnowledge> findAllOrderedByKeywordLength();
}
