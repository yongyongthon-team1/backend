package com.uniro.backend.domain.coupon.service;

import com.uniro.backend.common.ApiException;
import com.uniro.backend.domain.coupon.dto.CouponCheckResponse;
import com.uniro.backend.domain.coupon.entity.Coupon;
import com.uniro.backend.domain.coupon.repository.CouponRepository;
import com.uniro.backend.domain.store.repository.StoreRepository;
import com.uniro.backend.domain.store.support.StoreErrors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// 사장님이 학생이 보여 준 쿠폰을 확인하고 사용 처리한다
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final StoreRepository storeRepository;

    // 쿠폰 코드 확인. 내 가게의 쿠폰이면 사용·만료 상태여도 정보를 돌려준다.
    @Transactional(readOnly = true)
    public CouponCheckResponse check(Long ownerId, String code) {
        Coupon coupon = findMyStoreCoupon(ownerId, code);

        return CouponCheckResponse.of(
                coupon, coupon.statusAt(LocalDateTime.now(Coupon.ZONE)));
    }

    // 사장님의 가게에 속한 쿠폰을 찾는다 (다른 가게 쿠폰이면 403)
    private Coupon findMyStoreCoupon(Long ownerId, String code) {
        String trimmed = code == null ? "" : code.trim();

        if (trimmed.length() != Coupon.CODE_LENGTH) {
            throw StoreErrors.invalid(
                    "coupon_code", Coupon.CODE_LENGTH + "자리 코드를 입력해 주세요");
        }

        Long storeId = storeRepository.findIdByOwnerId(ownerId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "E404", "연결한 가게가 없습니다"));

        Coupon coupon = couponRepository.findByCode(trimmed)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "E404", "존재하지 않는 쿠폰 코드입니다"));

        if (!coupon.getStoreId().equals(storeId)) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN, "E403_OTHER_STORE", "다른 가게의 쿠폰입니다");
        }

        return coupon;
    }
}
