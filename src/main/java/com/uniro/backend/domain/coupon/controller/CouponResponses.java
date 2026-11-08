package com.uniro.backend.domain.coupon.controller;

import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.Map;

// 쿠폰 컨트롤러들이 함께 쓰는 성공 응답 만들기
final class CouponResponses {

    private CouponResponses() {
    }

    static ResponseEntity<Map<String, Object>> ok(String message, Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "S200");
        body.put("message", message);
        body.put("data", data);

        return ResponseEntity.ok(body);
    }
}
