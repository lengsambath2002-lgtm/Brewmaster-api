package com.sambath.admincafe.superadmin.dto;

import java.math.BigDecimal;
import java.util.List;

public record PlatformStats(
        long totalTenants,
        long activeTenants,
        long totalOrders,
        BigDecimal totalRevenue,
        List<TenantBreakdown> perTenant
) {
}
