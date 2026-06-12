package com.sambath.admincafe.report.dto;

import java.math.BigDecimal;

public record SalesLineItemResponse(
        String itemNo,
        String itemName,
        String itemDescription,
        BigDecimal price,
        long quantity,
        BigDecimal total
) {
}
