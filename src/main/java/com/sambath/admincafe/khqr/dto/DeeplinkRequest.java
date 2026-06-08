package com.sambath.admincafe.khqr.dto;

import jakarta.validation.constraints.NotBlank;

public record DeeplinkRequest(
        @NotBlank String qr,
        String url,
        String appName,
        String appIconUrl,
        String appCallback
) {}
