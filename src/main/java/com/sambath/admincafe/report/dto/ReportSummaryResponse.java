package com.sambath.admincafe.report.dto;

import java.math.BigDecimal;

public record ReportSummaryResponse(
        String range,
        BigDecimal totalRevenue,
        BigDecimal revenueGrowthPct,
        long totalOrders,
        BigDecimal ordersGrowthPct,
        int activeOrders,
        NamedCountResponse topSellingProduct,
        CategoryShareResponse topCategory
) {
}
