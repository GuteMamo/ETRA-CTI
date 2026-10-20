package com.etra.cti.evidence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EvidenceNoteRepository extends JpaRepository<EvidenceNote, UUID> {
    List<EvidenceNote> findByEvidenceIdOrderByCreatedAtDesc(UUID evidenceId);
}
