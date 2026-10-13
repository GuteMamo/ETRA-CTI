package com.etra.cti.remediation.dto;

import com.etra.cti.remediation.Retest;
import com.etra.cti.remediation.RetestResult;

import java.time.Instant;
import java.util.UUID;

public record RetestDto(
    UUID id, UUID findingId, UUID remediationId, UUID performedBy,
    RetestResult result, String notes, Instant createdAt
) {
    public static RetestDto from(Retest r) {
        return new RetestDto(r.getId(), r.getFindingId(), r.getRemediationId(),
            r.getPerformedBy(), r.getResult(), r.getNotes(), r.getCreatedAt());
    }
}
