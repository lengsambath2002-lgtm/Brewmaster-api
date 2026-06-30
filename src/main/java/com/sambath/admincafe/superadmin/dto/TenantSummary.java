package com.sambath.admincafe.superadmin.dto;

import java.time.Instant;

public record TenantSummary(
        Long id,
        String slug,
        String name,
        boolean active,
        Instant createdAt,
        long userCount,
        long productCount,
        long orderCount
) {
}
