package com.etra.cti.asset.dto;

import com.etra.cti.asset.AssetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAssetRequest(
    @NotNull AssetType type,
    @Size(max = 255) String hostname,
    @Size(max = 1000) String url,
    Integer port,
    @Size(max = 10) String protocol,
    @Size(max = 20) String environment,
    @Size(max = 10) String criticality
) {}
