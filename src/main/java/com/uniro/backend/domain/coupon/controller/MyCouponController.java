package com.uniro.backend.domain.coupon.controller;

import com.uniro.backend.common.TokenAuthenticator;
import com.uniro.backend.domain.coupon.service.CouponService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// 고교생의 내 쿠폰 API
@RestController
@RequestMapping("/users/me/coupons")
@RequiredArgsConstructor
public class MyCouponController {

    private final CouponService couponService;
    private final TokenAuthenticator authenticator;

    // 내 쿠폰 조회
    @Operation(
            summary = "내 쿠폰 조회",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping
    public ResponseEntity<Map<String, Object>> getMine(
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @RequestParam(name = "tour_id", required = false) Long tourId,
            @RequestParam(name = "store_id", required = false) Long storeId
    ) {
        Long userId = authenticator.requireHighSchool(authorization).getId();

        return CouponResponses.ok("조회 성공", Map.of(
                "items", couponService.myCoupons(userId, tourId, storeId)));
    }
}
