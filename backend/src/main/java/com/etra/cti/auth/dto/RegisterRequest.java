package com.etra.cti.auth.dto;

import com.etra.cti.user.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @Email @NotBlank String email,
    @NotBlank @Size(min = 8, max = 100) String password,
    @NotBlank @Size(max = 150) String fullName,
    UserRole role
) {
    public RegisterRequest(String email, String password, String fullName) {
        this(email, password, fullName, null);
    }
}
