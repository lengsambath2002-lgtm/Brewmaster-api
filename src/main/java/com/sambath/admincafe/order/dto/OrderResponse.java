package com.sambath.admincafe.order.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderResponse(
        String id,
        String tableNumber,
        boolean isTakeout,
        String customerName,
        String timeElapsed,
        String timestamp,
        String status,
        String server,
        List<OrderItemResponse> items,
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal total,
        String kitchenNote
) {
}
