package com.sambath.admincafe.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record PlaceOrderItem(
        @NotBlank String productName,
        @Min(1) int quantity,
        String size,
        List<String> notes,
        @NotNull @DecimalMin("0.00") BigDecimal priceOrder
) {
}
