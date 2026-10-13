package com.etra.cti.mobile.dto;

import com.etra.cti.mobile.MobilePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record RegisterMobileAppRequest(
    @NotNull UUID applicationId,
    @NotNull MobilePlatform platform,
    @NotBlank String packageName,
    String versionName,
    String versionCode,
    String fileName,
    Long sizeBytes,
    @NotBlank String sha256,
    Boolean signed,
    String minSdk,
    String targetSdk,
    List<String> permissions
) {}
