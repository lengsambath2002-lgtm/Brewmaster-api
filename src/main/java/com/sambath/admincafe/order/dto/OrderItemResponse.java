package com.sambath.admincafe.order.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderItemResponse(
        String id,
        String productName,
        int quantity,
        String size,
        List<String> notes,
        BigDecimal priceOrder
) {
}
