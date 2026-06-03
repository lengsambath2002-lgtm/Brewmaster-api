package com.sambath.admincafe.order.dto;

import jakarta.validation.Valid;

import java.util.List;

public record UpdateOrderRequest(
        String tableNumber,
        String customerName,
        Boolean isTakeout,
        String kitchenNote,
        @Valid List<PlaceOrderItem> items
) {
}
