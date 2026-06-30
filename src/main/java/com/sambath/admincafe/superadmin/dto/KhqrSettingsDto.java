package com.sambath.admincafe.superadmin.dto;

public record KhqrSettingsDto(
        String bakongAccountId,
        String merchantName,
        String merchantCity,
        String acquiringBank,
        String merchantId,
        String merchantCategoryCode,
        String currency,
        String storeLabel,
        String terminalLabel,
        String mobileNumber
) {
}
