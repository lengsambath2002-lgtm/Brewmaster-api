package com.sambath.admincafe.report.dto;

public record NamedCountResponse(
        String name,
        long unitsSold
) {
}
