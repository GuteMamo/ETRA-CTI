package com.etra.cti.investigation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InvestigationFindingRepository extends JpaRepository<InvestigationFinding, UUID> {
    List<InvestigationFinding> findByInvestigationId(UUID investigationId);
    boolean existsByInvestigationIdAndFindingId(UUID investigationId, UUID findingId);
}
