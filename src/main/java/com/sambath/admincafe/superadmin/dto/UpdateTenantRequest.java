package com.sambath.admincafe.superadmin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTenantRequest(
        @NotBlank @Size(max = 128) String name,
        @Valid KhqrSettingsDto khqr
) {
}
