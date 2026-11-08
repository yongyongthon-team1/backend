package com.uniro.backend.domain.coupon.controller;

import com.uniro.backend.common.TokenAuthenticator;
import com.uniro.backend.domain.coupon.service.CouponService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final TokenAuthenticator authenticator;

    // 쿠폰 코드 확인
    @Operation(
            summary = "쿠폰 코드 확인",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{couponCode}")
    public ResponseEntity<Map<String, Object>> check(
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @PathVariable String couponCode
    ) {
        return ok(couponService.check(
                authenticator.requireOwner(authorization).getId(), couponCode));
    }

    // 쿠폰 사용 처리
    @Operation(
            summary = "쿠폰 사용 처리",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{couponCode}/redeem")
    public ResponseEntity<Map<String, Object>> redeem(
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @PathVariable String couponCode
    ) {
        return ok("사용 처리 완료", couponService.redeem(
                authenticator.requireOwner(authorization).getId(), couponCode));
    }

    private ResponseEntity<Map<String, Object>> ok(Object data) {
        return ok("조회 성공", data);
    }

    private ResponseEntity<Map<String, Object>> ok(String message, Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "S200");
        body.put("message", message);
        body.put("data", data);

        return ResponseEntity.ok(body);
    }
}
