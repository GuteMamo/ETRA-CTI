package com.etra.cti.integration.dto;

import com.etra.cti.integration.ImportStatus;
import com.etra.cti.integration.IntegrationImport;
import com.etra.cti.integration.IntegrationSource;

import java.time.Instant;
import java.util.UUID;

public record ImportDto(
    UUID id, IntegrationSource source, UUID applicationId,
    String filename, Long sizeBytes,
    Integer findingsCreated, Integer findingsDuplicated, Integer assetsCreated,
    ImportStatus status, String errorMessage,
    Instant startedAt, Instant finishedAt, Long durationMs
) {
    public static ImportDto from(IntegrationImport i) {
        return new ImportDto(i.getId(), i.getSource(), i.getApplicationId(),
            i.getFilename(), i.getSizeBytes(),
            i.getFindingsCreated(), i.getFindingsDuplicated(), i.getAssetsCreated(),
            i.getStatus(), i.getErrorMessage(),
            i.getStartedAt(), i.getFinishedAt(), i.getDurationMs());
    }
}
