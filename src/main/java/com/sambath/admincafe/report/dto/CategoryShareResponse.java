package com.sambath.admincafe.report.dto;

import java.math.BigDecimal;

public record CategoryShareResponse(
        String name,
        BigDecimal sharePct
) {
}
