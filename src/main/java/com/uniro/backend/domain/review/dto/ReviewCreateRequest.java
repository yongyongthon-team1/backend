package com.uniro.backend.domain.review.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReviewCreateRequest(
        @JsonProperty("coupon_id") Long couponId,
        // 4.9 같은 소수가 조용히 4로 잘리지 않도록 숫자 그대로 받아 서비스에서 정수인지 확인한다
        Number rating,
        String content
) {
}
