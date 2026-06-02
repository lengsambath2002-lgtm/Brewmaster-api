package com.sambath.admincafe.transaction.dto;

import java.math.BigDecimal;

public record TransactionResponse(
        String id,
        String orderId,
        String customerName,
        String description,
        String timestamp,
        int itemsCount,
        BigDecimal amount,
        String status
) {
}
