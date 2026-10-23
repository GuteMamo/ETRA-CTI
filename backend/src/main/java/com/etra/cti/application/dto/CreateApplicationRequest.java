package com.etra.cti.application.dto;

import com.etra.cti.application.ApplicationType;
import com.etra.cti.application.Criticality;
import com.etra.cti.application.Environment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateApplicationRequest(
    @NotBlank @Size(max = 200) String name,
    @NotNull ApplicationType type,
    @NotNull Environment environment,
    @NotNull Criticality criticality,
    @Size(max = 500) String baseUrl,
    UUID projectId
) {
    public CreateApplicationRequest(String name, ApplicationType type, Environment environment, Criticality criticality, String baseUrl) {
        this(name, type, environment, criticality, baseUrl, null);
    }
}

