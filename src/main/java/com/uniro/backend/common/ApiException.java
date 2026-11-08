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

    // 요청 값 오류(400): 어떤 필드가 왜 잘못됐는지 errors에 담긴다
    public static ApiException invalid(String field, String reason) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "E400",
                "요청 값이 유효하지 않습니다",
                field,
                reason
        );
    }
}