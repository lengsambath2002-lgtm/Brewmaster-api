package com.sambath.admincafe.report.dto;

import java.math.BigDecimal;

public record TopProductResponse(
        String productName,
        String category,
        long unitsSold,
        BigDecimal revenue
) {
}
