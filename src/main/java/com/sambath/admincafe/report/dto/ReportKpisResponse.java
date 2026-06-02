package com.sambath.admincafe.report.dto;

import java.math.BigDecimal;

public record ReportKpisResponse(
        String range,
        BigDecimal avgOrderValue,
        BigDecimal avgOrderValueGrowthPct,
        long newCustomers,
        BigDecimal newCustomersGrowthPct,
        String topCategory,
        BigDecimal topCategorySharePct,
        BigDecimal staffEfficiencyMinutes
) {
}
