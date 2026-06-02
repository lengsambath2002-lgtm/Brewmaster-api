package com.sambath.admincafe.product.dto;

import java.math.BigDecimal;

public record ProductResponse(
        String id,
        String name,
        String category,
        BigDecimal price,
        int stock,
        String description,
        String image
) {
}
