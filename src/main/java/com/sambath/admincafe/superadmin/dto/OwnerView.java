package com.sambath.admincafe.superadmin.dto;

public record OwnerView(
        Long id,
        String email,
        String name,
        String role
) {
}
