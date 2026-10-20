package com.etra.cti.application.apikey.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateApiKeyRequest(
    @NotBlank @Size(max = 100)
    String name,
    Integer expiryDays
) {}
