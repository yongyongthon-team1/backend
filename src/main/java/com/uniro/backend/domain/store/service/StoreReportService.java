package com.uniro.backend.domain.store.service;

import com.uniro.backend.common.ApiException;
import com.uniro.backend.common.Kst;
import com.uniro.backend.domain.coupon.entity.CouponStatus;
import com.uniro.backend.domain.coupon.repository.CouponRepository;
import com.uniro.backend.domain.review.repository.StoreReviewRepository;
import com.uniro.backend.domain.review.repository.StoreReviewRepository.ReviewStats;
import com.uniro.backend.domain.store.dto.StoreReportResponse;
import com.uniro.backend.domain.store.repository.StoreRepository;
import com.uniro.backend.domain.store.support.StoreErrors;
import com.uniro.backend.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

// 사장님의 방문·리뷰 리포트
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreReportService {

    // 기본 조회 기간(일)과 한 번에 볼 수 있는 가장 긴 기간(일)
    private static final int DEFAULT_DAYS = 30;
    private static final int MAX_DAYS = 366;

    private final StoreRepository storeRepository;
    private final CouponRepository couponRepository;
    private final StoreReviewRepository reviewRepository;

    // 기간은 양 끝 날짜를 포함한다. to를 안 주면 오늘, from을 안 주면 최근 30일이 된다.
    public StoreReportResponse report(User owner, LocalDate from, LocalDate to) {
        // 기간 끝에 하루를 더하는 계산이 넘치지 않도록 날짜 범위부터 확인한다
        if (from != null) {
            StoreErrors.requireReasonableDate("from", from);
        }
        if (to != null) {
            StoreErrors.requireReasonableDate("to", to);
        }

        LocalDate end = to == null ? Kst.now().toLocalDate() : to;
        LocalDate start = from == null ? end.minusDays(DEFAULT_DAYS - 1L) : from;

        if (start.isAfter(end)) {
            throw StoreErrors.invalid("from", "from은 to보다 늦을 수 없어요");
        }
        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_DAYS) {
            throw StoreErrors.invalid("from", "조회 기간은 최대 " + MAX_DAYS + "일이에요");
        }

        Long storeId = storeRepository.findIdByOwnerId(owner.getId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "E404", "연결한 가게가 없습니다"));

        long visits = couponRepository.countByStoreIdAndStatusAndUsedDateBetween(
                storeId, CouponStatus.USED, start, end);
        // 리뷰는 시각으로 저장돼 있어서 마지막 날 끝까지 포함하도록 다음 날 0시 앞까지로 센다
        ReviewStats reviews = reviewRepository.statsBetween(
                storeId, start.atStartOfDay(), end.plusDays(1).atStartOfDay());

        return new StoreReportResponse(
                start, end, visits, reviews.count(), reviews.roundedAverage());
    }
}
