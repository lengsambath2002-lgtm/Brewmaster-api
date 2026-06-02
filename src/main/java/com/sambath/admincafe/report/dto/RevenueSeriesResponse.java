package com.sambath.admincafe.report.dto;

import java.util.List;

public record RevenueSeriesResponse(
        String range,
        List<RevenuePointResponse> points
) {
}
