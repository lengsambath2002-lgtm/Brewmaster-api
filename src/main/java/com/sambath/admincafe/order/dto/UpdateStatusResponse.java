package com.sambath.admincafe.order.dto;

import com.sambath.admincafe.transaction.dto.TransactionResponse;

public record UpdateStatusResponse(
        OrderResponse order,
        TransactionResponse transaction
) {
}
