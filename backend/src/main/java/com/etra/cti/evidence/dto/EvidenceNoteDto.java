package com.etra.cti.evidence.dto;

import com.etra.cti.evidence.EvidenceNote;

import java.time.Instant;
import java.util.UUID;

public record EvidenceNoteDto(
    UUID id, UUID evidenceId, UUID authorId, String note, Instant createdAt
) {
    public static EvidenceNoteDto from(EvidenceNote n) {
        return new EvidenceNoteDto(n.getId(), n.getEvidenceId(), n.getAuthorId(), n.getNote(), n.getCreatedAt());
    }
}
