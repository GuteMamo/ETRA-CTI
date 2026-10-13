package com.etra.cti.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
    @Email @NotBlank String email,
    @NotBlank @Size(min = 8) String password,
    @NotBlank @Size(max = 150) String fullName,
    @NotBlank String role
) {}
