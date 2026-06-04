package com.sambath.admincafe.product.dto;

import java.math.BigDecimal;

public record ProductResponse(
        String id,
        String name,
        String category,
        BigDecimal price,
        boolean locked,
        String image
) {
}
