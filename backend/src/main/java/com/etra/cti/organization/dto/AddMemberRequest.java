package com.etra.cti.organization.dto;

import com.etra.cti.user.UserRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddMemberRequest(
    @NotNull UUID userId,
    @NotNull UserRole role
) {}
