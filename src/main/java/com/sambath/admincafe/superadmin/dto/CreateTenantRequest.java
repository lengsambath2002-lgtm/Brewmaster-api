package com.sambath.admincafe.superadmin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(
        @NotBlank @Size(max = 64) String slug,
        @NotBlank @Size(max = 128) String name,
        @Valid KhqrSettingsDto khqr,
        @NotBlank @Email @Size(max = 255) String ownerEmail,
        @NotBlank @Size(max = 128) String ownerName,
        @NotBlank @Size(min = 6, max = 72) String ownerPassword
) {
}
