package com.sambath.admincafe.superadmin.dto;

import java.math.BigDecimal;

public record TenantBreakdown(
        Long id,
        String slug,
        String name,
        boolean active,
        long orders,
        BigDecimal revenue
) {
}
