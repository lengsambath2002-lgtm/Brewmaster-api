package com.sambath.admincafe.khqr.dto;

import jakarta.validation.constraints.NotBlank;

public record GenerateIndividualRequest(
        @NotBlank String bakongAccountId,
        @NotBlank String merchantName,
        String merchantCity,
        String acquiringBank,
        String accountInformation,
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
