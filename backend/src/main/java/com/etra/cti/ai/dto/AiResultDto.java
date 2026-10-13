package com.etra.cti.ai.dto;

public record AiResultDto(
    String analysisType,
    String resourceType,
    String resourceId,
    String result,
    String model,
    boolean fromCache
) {}
