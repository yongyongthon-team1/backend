package com.uniro.backend.domain.coupon.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.uniro.backend.domain.coupon.entity.Coupon;
import com.uniro.backend.domain.coupon.entity.CouponStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;

// 사장님이 쿠폰 코드를 확인했을 때의 응답
public record CouponCheckResponse(
        @JsonProperty("coupon_id") Long couponId,
        String code,
        @JsonProperty("discount_text") String discountText,
        @JsonProperty("tour_date") LocalDate tourDate,
        CouponStatus status,
        @JsonProperty("valid_until") OffsetDateTime validUntil
) {
    public static CouponCheckResponse of(Coupon coupon, CouponStatus status) {
        return new CouponCheckResponse(
                coupon.getId(),
                coupon.getCode(),
                coupon.getDiscountSnapshot(),
                coupon.tourDate(),
                status,
                coupon.validUntil()
        );
    }
}
