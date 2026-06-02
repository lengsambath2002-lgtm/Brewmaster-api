package com.sambath.admincafe.report.dto;

import java.math.BigDecimal;

public record RevenuePointResponse(
        String label,
        String date,
        BigDecimal revenue,
        boolean isPeak
) {
}
