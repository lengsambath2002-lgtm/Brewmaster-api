package com.sambath.admincafe.order.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderResponse(
        String id,
        Integer orderNumber,
        String orderDate,
        String tableNumber,
        boolean isTakeout,
        boolean guest,
        String customerName,
        String timeElapsed,
        String timestamp,
        String status,
        String server,
        List<OrderItemResponse> items,
        BigDecimal subtotal,
        BigDecimal total,
        String kitchenNote,
        String paymentStatus,
        String paidAt
) {
}
