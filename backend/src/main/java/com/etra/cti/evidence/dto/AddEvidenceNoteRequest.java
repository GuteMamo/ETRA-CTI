package com.etra.cti.evidence.dto;

import jakarta.validation.constraints.NotBlank;

public record AddEvidenceNoteRequest(@NotBlank String note) {}
