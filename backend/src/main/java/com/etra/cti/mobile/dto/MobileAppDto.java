package com.etra.cti.mobile.dto;

import com.etra.cti.mobile.MobileApp;
import com.etra.cti.mobile.MobilePlatform;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MobileAppDto(
    UUID id, UUID applicationId, MobilePlatform platform,
    String packageName, String versionName, String versionCode,
    String fileName, Long sizeBytes, String sha256,
    Boolean signed, String minSdk, String targetSdk,
    List<String> permissions, Instant createdAt
) {
    public static MobileAppDto from(MobileApp m) {
        return new MobileAppDto(m.getId(), m.getApplicationId(), m.getPlatform(),
            m.getPackageName(), m.getVersionName(), m.getVersionCode(),
            m.getFileName(), m.getSizeBytes(), m.getSha256(),
            m.getSigned(), m.getMinSdk(), m.getTargetSdk(),
            m.getPermissions(), m.getCreatedAt());
    }
}
