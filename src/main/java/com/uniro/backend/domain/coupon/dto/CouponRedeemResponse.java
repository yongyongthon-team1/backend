package com.uniro.backend.domain.coupon.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.uniro.backend.domain.coupon.entity.Coupon;
import com.uniro.backend.domain.coupon.entity.CouponStatus;

import java.time.OffsetDateTime;

// 쿠폰 사용 처리 결과
public record CouponRedeemResponse(
        @JsonProperty("coupon_id") Long couponId,
        CouponStatus status,
        @JsonProperty("used_at") OffsetDateTime usedAt
) {
    public static CouponRedeemResponse of(Coupon coupon) {
        return new CouponRedeemResponse(
                coupon.getId(),
                coupon.getStatus(),
                coupon.getUsedAt().atZone(Coupon.ZONE).toOffsetDateTime()
        );
    }
}
