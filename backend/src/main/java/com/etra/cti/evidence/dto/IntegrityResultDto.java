package com.etra.cti.evidence.dto;

public record IntegrityResultDto(
    java.util.UUID evidenceId,
    String filename,
    String integrity,       // INTACT | TAMPERED | FILE_MISSING
    String storedSha256,
    String currentSha256,
    long sizeBytes
) {}
