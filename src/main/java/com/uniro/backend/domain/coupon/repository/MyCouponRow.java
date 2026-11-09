package com.uniro.backend.domain.coupon.repository;

import com.uniro.backend.domain.coupon.entity.Coupon;

// 내 쿠폰 조회 결과 한 줄: 쿠폰, 그 쿠폰을 받은 신청의 탐방 ID와 승인 여부, 상점 이름
public record MyCouponRow(
        Coupon coupon,
        Long tourId,
        boolean applicationApproved,
        String storeName
) {
}
