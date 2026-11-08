package com.uniro.backend.domain.coupon.service;

import com.uniro.backend.common.ApiException;
import com.uniro.backend.domain.coupon.dto.CouponCheckResponse;
import com.uniro.backend.domain.coupon.dto.CouponRedeemResponse;
import com.uniro.backend.domain.coupon.entity.Coupon;
import com.uniro.backend.domain.coupon.entity.CouponStatus;
import com.uniro.backend.domain.coupon.repository.CouponRepository;
import com.uniro.backend.domain.store.repository.StoreRepository;
import com.uniro.backend.domain.store.support.StoreErrors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

// 사장님이 학생이 보여 준 쿠폰을 확인하고 사용 처리한다
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final StoreRepository storeRepository;

    // 쿠폰 코드 확인. 내 가게의 쿠폰이면 사용·만료 상태여도 정보를 돌려준다.
    @Transactional(readOnly = true)
    public CouponCheckResponse check(Long ownerId, String code) {
        String trimmed = normalizeCode(code);
        Long storeId = findStoreId(ownerId);

        Coupon coupon = couponRepository.findByCode(trimmed)
                .orElseThrow(this::couponNotFound);
        requireSameStore(coupon.getStoreId(), storeId);

        return CouponCheckResponse.of(
                coupon, coupon.statusAt(LocalDateTime.now(Coupon.ZONE)));
    }

    // 쿠폰 사용 처리. 탐방 당일에만 쓸 수 있고, 같은 날 같은 가게에서는 한 장만 처리된다.
    @Transactional
    public CouponRedeemResponse redeem(Long ownerId, String code) {
        String trimmed = normalizeCode(code);
        Long storeId = findStoreId(ownerId);

        // 잠금 없이 존재와 소유부터 확인한다. 남의 쿠폰이나 없는 코드는 잠그지 않는다.
        Long couponStoreId = couponRepository.findStoreIdByCode(trimmed)
                .orElseThrow(this::couponNotFound);
        requireSameStore(couponStoreId, storeId);

        try {
            // 내 쿠폰만 행을 잠그고 다시 읽는다 (같은 쿠폰의 동시 요청을 막고 최신 상태를 본다)
            Coupon coupon = couponRepository.findByCodeForUpdate(trimmed)
                    .orElseThrow(this::couponNotFound);

            // DB 시각 정밀도(마이크로초)와 응답이 어긋나지 않도록 초 단위로 맞춘다
            LocalDateTime now = LocalDateTime.now(Coupon.ZONE)
                    .truncatedTo(ChronoUnit.SECONDS);

            validateUsable(coupon, now);

            coupon.use(now);
            couponRepository.saveAndFlush(coupon);

            return CouponRedeemResponse.of(coupon);
        } catch (DataIntegrityViolationException e) {
            // 거의 동시에 같은 가게의 다른 쿠폰이 먼저 사용 처리된 경우 (store_id, used_date 유니크)
            throw dailyLimit();
        } catch (PessimisticLockingFailureException e) {
            // 동시 요청과 겹쳐 DB가 데드락이나 잠금 대기 초과로 끊은 경우
            throw new ApiException(
                    HttpStatus.CONFLICT, "E409", "잠시 후 다시 시도해 주세요");
        }
    }

    // 사용할 수 있는 쿠폰인지 규칙 순서대로 확인한다
    private void validateUsable(Coupon coupon, LocalDateTime now) {
        CouponStatus status = coupon.statusAt(now);

        if (status == CouponStatus.USED) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "E409_ALREADY_USED", "이미 사용된 쿠폰입니다");
        }
        if (status == CouponStatus.EXPIRED) {
            throw new ApiException(
                    HttpStatus.GONE, "E410_EXPIRED", "만료된 쿠폰입니다");
        }
        if (now.toLocalDate().isBefore(coupon.tourDate())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "E409_NOT_YET_VALID",
                    "탐방 당일에만 사용할 수 있어요"
            );
        }
        if (couponRepository.existsByStoreIdAndUsedDate(
                coupon.getStoreId(), now.toLocalDate())) {
            throw dailyLimit();
        }
    }

    // 코드 형식을 확인하고 공백을 없앤다
    private String normalizeCode(String code) {
        String trimmed = code == null ? "" : code.trim();

        if (trimmed.length() != Coupon.CODE_LENGTH) {
            throw StoreErrors.invalid(
                    "coupon_code", Coupon.CODE_LENGTH + "자리 코드를 입력해 주세요");
        }

        return trimmed;
    }

    private Long findStoreId(Long ownerId) {
        return storeRepository.findIdByOwnerId(ownerId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "E404", "연결한 가게가 없습니다"));
    }

    // 다른 가게의 쿠폰이면 403
    private void requireSameStore(Long couponStoreId, Long storeId) {
        if (!couponStoreId.equals(storeId)) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN, "E403_OTHER_STORE", "다른 가게의 쿠폰입니다");
        }
    }

    private ApiException couponNotFound() {
        return new ApiException(
                HttpStatus.NOT_FOUND, "E404", "존재하지 않는 쿠폰 코드입니다");
    }

    private ApiException dailyLimit() {
        return new ApiException(
                HttpStatus.CONFLICT,
                "E409_DAILY_LIMIT",
                "오늘 이미 다른 쿠폰이 사용되었습니다"
        );
    }
}
