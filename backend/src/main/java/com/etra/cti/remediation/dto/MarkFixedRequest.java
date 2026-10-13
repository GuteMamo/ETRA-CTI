package com.etra.cti.remediation.dto;

public record MarkFixedRequest(
    String fixNotes,
    String commitRef
) {}
