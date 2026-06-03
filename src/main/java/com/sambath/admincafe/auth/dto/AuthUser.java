package com.sambath.admincafe.auth.dto;

public record AuthUser(
        String id,
        String email,
        String name,
        String role
) {
}
