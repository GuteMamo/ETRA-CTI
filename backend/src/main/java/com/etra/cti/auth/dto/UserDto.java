package com.etra.cti.auth.dto;

import com.etra.cti.user.User;
import com.etra.cti.user.UserRole;

import java.util.UUID;

public record UserDto(UUID id, String email, String fullName, UserRole role, boolean enabled) {
    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.isEnabled());
    }
}
