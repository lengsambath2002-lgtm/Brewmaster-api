package com.sambath.admincafe.auth.dto;

public record AuthUser(
        String id,
        Long tenantId,
        String email,
        String name,
        String role
) {
}
