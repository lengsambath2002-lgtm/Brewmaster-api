package com.sambath.admincafe.report.dto;

import java.math.BigDecimal;
import java.util.List;

public record SalesReportResponse(
        String range,
        String periodStart,
        String periodEnd,
        String salesPerson,
        BigDecimal salesTotal,
        List<SalesLineItemResponse> lineItems
) {
}
