package com.uniro.backend.domain.coupon.service;

import com.uniro.backend.common.ApiException;
import com.uniro.backend.domain.coupon.dto.CouponCheckResponse;
import com.uniro.backend.domain.coupon.dto.CouponRedeemResponse;
import com.uniro.backend.domain.coupon.dto.MyCouponResponse;
import com.uniro.backend.domain.coupon.entity.Coupon;
import com.uniro.backend.domain.coupon.entity.CouponStatus;
import com.uniro.backend.domain.coupon.repository.CouponRepository;
import com.uniro.backend.domain.coupon.repository.MyCouponRow;
import com.uniro.backend.domain.review.repository.StoreReviewRepository;
import com.uniro.backend.domain.store.repository.StoreRepository;
import com.uniro.backend.domain.store.support.StoreErrors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// 사장님이 학생이 보여 준 쿠폰을 확인하고 사용 처리한다
@Service
@RequiredArgsConstructor
public class CouponService {

    // 헷갈리기 쉬운 0/O, 1/I를 뺀 코드 문자
    private static final String CODE_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CouponRepository couponRepository;
    private final StoreRepository storeRepository;
    private final StoreReviewRepository reviewRepository;

    // 내 쿠폰 조회 (고교생). 유효기간이 지난 발급 쿠폰은 EXPIRED로 내려간다.
    @Transactional(readOnly = true)
    public List<MyCouponResponse> myCoupons(Long userId, Long tourId, Long storeId) {
        LocalDateTime now = LocalDateTime.now(Coupon.ZONE);

        List<MyCouponRow> rows = couponRepository.findMine(userId, tourId, storeId);
        if (rows.isEmpty()) {
            return List.of();
        }

        // 쿠폰마다 따로 묻지 않고, 리뷰를 쓴 쿠폰을 한 번에 가져온다
        Set<Long> reviewed = new HashSet<>(reviewRepository.findReviewedCouponIds(
                rows.stream().map(r -> r.coupon().getId()).toList()));

        return rows.stream()
                .map(r -> MyCouponResponse.of(
                        r.coupon(), r.tourId(), r.storeName(), statusFor(r, now),
                        reviewed.contains(r.coupon().getId())))
                .toList();
    }

    // 신청이 취소될 때 호출한다 (탐방 취소 T-05, 신청 취소 A-03).
    // 주어진 신청들의 아직 쓰지 않은 쿠폰을 만료로 바꾸고, 바뀐 쿠폰 수를 돌려준다.
    // 호출한 쪽의 트랜잭션에 참여하므로, 신청 취소와 함께 커밋되거나 함께 되돌아간다.
    @Transactional
    public int expireIssuedCoupons(Collection<Long> applicationIds) {
        if (applicationIds == null || applicationIds.isEmpty()) {
            return 0;
        }

        return couponRepository.expireIssuedByApplicationIds(applicationIds);
    }

    // 쿠폰 발급 대상: 코스 안의 제휴 상점과 그 제휴 행사
    public record CouponTarget(Long storeId, Long partnershipId, String discount) {
    }

    // 신청마다 제휴 상점 하나에 쿠폰 한 장씩 발급하고, 발급한 쿠폰 수를 돌려준다.
    // 호출한 쪽의 트랜잭션에 참여하므로, 코스 확정과 함께 커밋되거나 함께 되돌아간다.
    @Transactional
    public int issueCoupons(
            Collection<Long> applicationIds, Collection<CouponTarget> targets, LocalDate tourDate
    ) {
        int total = applicationIds.size() * targets.size();
        if (total == 0) {
            return 0;
        }

        LocalDateTime expiresAt = tourDate.atTime(23, 59, 59);
        Iterator<String> codes = newCodes(total).iterator();
        List<Coupon> coupons = new ArrayList<>(total);

        for (Long applicationId : applicationIds) {
            for (CouponTarget target : targets) {
                coupons.add(new Coupon(
                        applicationId, target.storeId(), target.partnershipId(),
                        codes.next(), target.discount(), expiresAt));
            }
        }

        try {
            couponRepository.saveAllAndFlush(coupons);
        } catch (DataIntegrityViolationException e) {
            // 다른 요청이 같은 코드를 방금 쓴 드문 경우
            throw new ApiException(
                    HttpStatus.CONFLICT, "E409", "잠시 후 다시 시도해 주세요");
        }

        return total;
    }

    // 서로 겹치지 않고 아직 쓰이지 않은 쿠폰 코드를 count개 만든다
    private Set<String> newCodes(int count) {
        Set<String> codes = new LinkedHashSet<>();

        while (true) {
            while (codes.size() < count) {
                codes.add(randomCode());
            }

            List<String> taken = couponRepository.findExistingCodes(codes);
            if (taken.isEmpty()) {
                return codes;
            }
            taken.forEach(codes::remove);
        }
    }

    private String randomCode() {
        StringBuilder code = new StringBuilder(Coupon.CODE_LENGTH);
        for (int i = 0; i < Coupon.CODE_LENGTH; i++) {
            code.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return code.toString();
    }

    // 승인된 신청이 아니면(취소·거절) 아직 쓰지 않은 쿠폰은 만료로 본다.
    // 신청 취소 때 쿠폰을 만료 처리하지 못한 경우에도 유효한 쿠폰처럼 보이지 않게 한다.
    private CouponStatus statusFor(MyCouponRow row, LocalDateTime now) {
        CouponStatus status = row.coupon().statusAt(now);

        if (status == CouponStatus.ISSUED && !row.applicationApproved()) {
            return CouponStatus.EXPIRED;
        }

        return status;
    }

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
