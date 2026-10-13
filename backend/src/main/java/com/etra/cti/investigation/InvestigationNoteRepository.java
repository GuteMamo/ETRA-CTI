package com.etra.cti.investigation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InvestigationNoteRepository extends JpaRepository<InvestigationNote, UUID> {
    List<InvestigationNote> findByInvestigationIdOrderByCreatedAtAsc(UUID investigationId);
}
