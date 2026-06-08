package com.sambath.admincafe.khqr.dto;

import jakarta.validation.constraints.NotBlank;

public record GenerateMerchantRequest(
        @NotBlank String bakongAccountId,
        @NotBlank String merchantId,
        @NotBlank String acquiringBank,
        @NotBlank String merchantName,
        String merchantCity,
        String currency,
        Double amount,
        String billNumber,
        String mobileNumber,
        String storeLabel,
        String terminalLabel,
        String purposeOfTransaction,
        String upiAccountInformation,
        String merchantAlternateLanguagePreference,
        String merchantNameAlternateLanguage,
        String merchantCityAlternateLanguage,
        Long expirationTimestamp,
        String merchantCategoryCode
) {}
