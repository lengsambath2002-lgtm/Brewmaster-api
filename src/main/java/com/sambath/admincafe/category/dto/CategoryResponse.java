package com.sambath.admincafe.category.dto;

public record CategoryResponse(
        String id,
        String name,
        long itemsCount,
        String image,
        String icon
) {
}
