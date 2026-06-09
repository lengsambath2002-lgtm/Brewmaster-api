package com.sambath.admincafe.khqr.dto;

/**
 * Result of checking whether a KHQR payment has been completed.
 *
 * @param paid         true when the Bakong transaction was found (responseCode 0)
 * @param responseCode raw Bakong responseCode (0 = success/paid)
 * @param message      Bakong responseMessage, for diagnostics
 */
public record CheckResponse(boolean paid, Integer responseCode, String message) {}
