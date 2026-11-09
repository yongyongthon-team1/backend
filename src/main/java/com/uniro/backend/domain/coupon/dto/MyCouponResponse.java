package com.uniro.backend.domain.coupon.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.uniro.backend.domain.coupon.entity.Coupon;
import com.uniro.backend.domain.coupon.entity.CouponStatus;

import java.time.OffsetDateTime;

// 고교생이 내 쿠폰을 조회했을 때의 쿠폰 하나.
// reviewed는 이 쿠폰으로 리뷰를 이미 썼는지 여부다.
public record MyCouponResponse(
        @JsonProperty("coupon_id") Long couponId,
        String code,
        @JsonProperty("tour_id") Long tourId,
        StoreSummary store,
        @JsonProperty("discount_text") String discountText,
        CouponStatus status,
        @JsonProperty("valid_until") OffsetDateTime validUntil,
        @JsonProperty("used_at") OffsetDateTime usedAt,
        boolean reviewed
) {
    public record StoreSummary(
            @JsonProperty("store_id") Long storeId,
            String name
    ) {
    }

    public static MyCouponResponse of(
            Coupon coupon,
            Long tourId,
            String storeName,
            CouponStatus status,
            boolean reviewed
    ) {
        return new MyCouponResponse(
                coupon.getId(),
                coupon.getCode(),
                tourId,
                new StoreSummary(coupon.getStoreId(), storeName),
                coupon.getDiscountSnapshot(),
                status,
                coupon.validUntil(),
                coupon.usedAtOffset(),
                reviewed
        );
    }
}
