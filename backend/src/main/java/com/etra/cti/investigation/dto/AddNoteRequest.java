package com.etra.cti.investigation.dto;

import jakarta.validation.constraints.NotBlank;

public record AddNoteRequest(@NotBlank String note) {}
