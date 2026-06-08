package com.sambath.admincafe.khqr.dto;

public record OrderKhqrRequest(
        String currency,
        Double amount,
        String billNumber,
        Long expirationTimestamp
) {}
