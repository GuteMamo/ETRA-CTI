package com.etra.cti.remediation.dto;

import com.etra.cti.remediation.Remediation;
import com.etra.cti.remediation.RemediationStatus;

import java.time.Instant;
import java.util.UUID;

public record RemediationDto(
    UUID id, UUID findingId, UUID developerId,
    RemediationStatus status, String fixNotes, String commitRef,
    Instant startedAt, Instant fixedAt, Instant verifiedAt,
    Integer reopenedCount, Instant createdAt
) {
    public static RemediationDto from(Remediation r) {
        return new RemediationDto(r.getId(), r.getFindingId(), r.getDeveloperId(),
            r.getStatus(), r.getFixNotes(), r.getCommitRef(),
            r.getStartedAt(), r.getFixedAt(), r.getVerifiedAt(),
            r.getReopenedCount(), r.getCreatedAt());
    }
}
