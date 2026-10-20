package com.etra.cti.evidence.dto;

import com.etra.cti.evidence.Evidence;

import java.time.Instant;
import java.util.UUID;

public record EvidenceDto(
    UUID id, UUID findingId, String filename, String contentType,
    Long sizeBytes, String sha256, UUID createdBy, Instant createdAt
) {
    public static EvidenceDto from(Evidence e) {
        return new EvidenceDto(e.getId(), e.getFindingId(), e.getFilename(),
            e.getContentType(), e.getSizeBytes(), e.getSha256(),
            e.getCreatedBy(), e.getCreatedAt());
    }
}
