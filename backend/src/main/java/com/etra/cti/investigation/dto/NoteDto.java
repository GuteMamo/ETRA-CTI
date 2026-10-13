package com.etra.cti.investigation.dto;

import com.etra.cti.investigation.InvestigationNote;

import java.time.Instant;
import java.util.UUID;

public record NoteDto(UUID id, UUID investigationId, UUID authorId, String note, Instant createdAt) {
    public static NoteDto from(InvestigationNote n) {
        return new NoteDto(n.getId(), n.getInvestigationId(), n.getAuthorId(), n.getNote(), n.getCreatedAt());
    }
}
