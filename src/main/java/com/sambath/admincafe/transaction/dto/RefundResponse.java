package com.sambath.admincafe.transaction.dto;

import com.sambath.admincafe.order.dto.OrderResponse;

public record RefundResponse(
        TransactionResponse transaction,
        OrderResponse order
) {
}
