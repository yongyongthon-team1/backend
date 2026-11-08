package com.uniro.backend.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handle(
            ApiException e
    ) {
        Map<String, Object> body = new LinkedHashMap<>();

        body.put("code", e.getCode());
        body.put("message", e.getMessage());
        body.put("data", null);

        if (e.getField() != null) {
            body.put(
                    "errors",
                    List.of(Map.of(
                            "field", e.getField(),
                            "reason", e.getReason()
                    ))
            );
        }

        return ResponseEntity
                .status(e.getStatus())
                .body(body);
    }

    // 숫자 파라미터(page, size 등)에 숫자가 아닌 값이 올 때
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException e
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "E400");
        body.put("message", "요청 값이 유효하지 않습니다");
        body.put("data", null);
        body.put(
                "errors",
                List.of(Map.of(
                        "field", e.getName(),
                        "reason", "올바른 형식의 값을 입력해 주세요"
                ))
        );

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleUploadSize(
            MaxUploadSizeExceededException e
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "E400");
        body.put("message", "요청 값이 유효하지 않습니다");
        body.put("data", null);
        body.put(
                "errors",
                List.of(Map.of(
                        "field", "document",
                        "reason", "jpg, png 형식의 5MB 이하 파일만 올릴 수 있어요"
                ))
        );

        return ResponseEntity.badRequest().body(body);
    }
}