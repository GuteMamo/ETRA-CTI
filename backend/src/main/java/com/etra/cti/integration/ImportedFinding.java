package com.etra.cti.integration;

import com.etra.cti.detection.Severity;

/**
 * Common representation a parser produces from any tool.
 */
public record ImportedFinding(
    String title,
    String description,
    String endpoint,
    String parameter,
    Severity severity,
    String rawEvidence,
    String category
) {}
