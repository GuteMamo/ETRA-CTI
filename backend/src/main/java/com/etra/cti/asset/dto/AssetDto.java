package com.etra.cti.asset.dto;

import com.etra.cti.asset.Asset;
import com.etra.cti.asset.AssetType;

import java.util.UUID;

public record AssetDto(
    UUID id, UUID applicationId, AssetType type,
    String hostname, String url, Integer port,
    String protocol, String environment, String criticality, String status
) {
    public static AssetDto from(Asset a) {
        return new AssetDto(a.getId(), a.getApplicationId(), a.getType(),
            a.getHostname(), a.getUrl(), a.getPort(), a.getProtocol(),
            a.getEnvironment(), a.getCriticality(), a.getStatus());
    }
}
