package com.sambath.admincafe.superadmin.dto;

import java.time.Instant;
import java.util.List;

public record TenantDetail(
        Long id,
        String slug,
        String name,
        boolean active,
        Instant createdAt,
        KhqrSettingsDto khqr,
        List<OwnerView> owners
) {
}
