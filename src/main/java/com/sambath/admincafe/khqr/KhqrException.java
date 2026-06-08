package com.sambath.admincafe.khqr;

public class KhqrException extends RuntimeException {

    private final Integer errorCode;

    public KhqrException(Integer errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public Integer getErrorCode() {
        return errorCode;
    }
}
