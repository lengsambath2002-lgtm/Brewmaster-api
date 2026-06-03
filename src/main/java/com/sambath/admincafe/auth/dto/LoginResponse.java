package com.sambath.admincafe.auth.dto;

public record LoginResponse(
        String token,
        AuthUser user
) {
}
