package com.uniro.backend.domain.store.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 상점 컨트롤러에서만 적용되는 요청 형식 오류 처리.
// 공통 ApiExceptionHandler는 건드리지 않고, 다른 도메인의 응답에는 영향을 주지 않는다.
@Slf4j
@RestControllerAdvice(basePackages = "com.uniro.backend.domain.store")
public class StoreExceptionHandler {

    // 숫자 파라미터(page, size 등)에 숫자가 아닌 값이 올 때
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException e
    ) {
        return badRequest(e.getName(), "올바른 형식의 값을 입력해 주세요");
    }

    // JSON 형식이 깨졌거나 날짜 같은 값의 형식이 잘못된 요청 본문
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleNotReadable(
            HttpMessageNotReadableException e
    ) {
        // 응답에는 일반 문구만 담으므로, 원인은 서버 로그로 남긴다
        log.debug("요청 본문을 읽을 수 없습니다: {}", e.getMessage());

        return badRequest("body", "요청 본문의 형식이 올바르지 않습니다");
    }

    private ResponseEntity<Map<String, Object>> badRequest(
            String field, String reason
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "E400");
        body.put("message", "요청 값이 유효하지 않습니다");
        body.put("data", null);
        body.put("errors", List.of(Map.of("field", field, "reason", reason)));

        return ResponseEntity.badRequest().body(body);
    }
}
