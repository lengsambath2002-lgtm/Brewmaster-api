package com.sambath.admincafe.superadmin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 128) String name,
        @Size(max = 64) String role,
        @NotBlank @Size(min = 6, max = 72) String password
) {
}
