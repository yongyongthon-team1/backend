package com.uniro.backend.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final String field;
    private final String reason;

    public ApiException(
            HttpStatus status,
            String code,
            String message
    ) {
        this(status, code, message, null, null);
    }

    public ApiException(
            HttpStatus status,
            String code,
            String message,
            String field,
            String reason
    ) {
        super(message);
        this.status = status;
        this.code = code;
        this.field = field;
        this.reason = reason;
    }
}