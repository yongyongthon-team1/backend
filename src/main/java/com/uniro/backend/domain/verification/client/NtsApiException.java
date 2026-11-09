package com.uniro.backend.domain.verification.client;

public class NtsApiException extends RuntimeException {

    public NtsApiException(String message) {
        super(message);
    }

    public NtsApiException(String message, Throwable cause) {
        super(message, cause);
    }
}