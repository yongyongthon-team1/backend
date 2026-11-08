package com.uniro.backend.domain.review.service;

import com.uniro.backend.common.ApiException;
import com.uniro.backend.common.Kst;
import com.uniro.backend.domain.coupon.entity.CouponStatus;
import com.uniro.backend.domain.coupon.repository.CouponRepository;
import com.uniro.backend.domain.coupon.repository.CouponRepository.ReviewTarget;
import com.uniro.backend.domain.review.dto.ReviewCreateRequest;
import com.uniro.backend.domain.review.dto.ReviewCreateResponse;
import com.uniro.backend.domain.review.dto.ReviewListResponse;
import com.uniro.backend.domain.review.entity.StoreReview;
import com.uniro.backend.domain.review.entity.StoreReviewSummary;
import com.uniro.backend.domain.review.repository.StoreReviewRepository;
import com.uniro.backend.domain.review.repository.StoreReviewSummaryRepository;
import com.uniro.backend.domain.store.repository.StoreRepository;
import com.uniro.backend.domain.store.support.StoreErrors;
import com.uniro.backend.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 상점 리뷰 작성·조회
@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final StoreRepository storeRepository;
    private final CouponRepository couponRepository;
    private final StoreReviewRepository reviewRepository;
    private final StoreReviewSummaryRepository summaryRepository;

    // 리뷰 작성 (고교생). 쿠폰을 사용 처리한 상점에만, 쿠폰 한 장에 한 번 쓸 수 있다.
    @Transactional
    public ReviewCreateResponse create(User student, Long storeId, ReviewCreateRequest request) {
        Long couponId = requireCouponId(request.couponId());
        int rating = requireRating(request.rating());
        String content = normalizeContent(request.content());

        requireStore(storeId);

        // 없는 쿠폰, 남의 쿠폰, 다른 상점의 쿠폰, 사용 전 쿠폰은 모두 같은 응답으로 막는다
        // (쿠폰 ID가 있는지 없는지 알아낼 수 없게 한다).
        ReviewTarget target = couponRepository.findReviewTarget(couponId)
                .filter(t -> t.userId().equals(student.getId())
                        && t.storeId().equals(storeId)
                        && t.status() == CouponStatus.USED)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.FORBIDDEN, "E403_COUPON_NOT_USED",
                        "쿠폰을 사용한 상점에만 리뷰를 쓸 수 있어요"));

        if (reviewRepository.existsByCouponIdOrUserIdAndTourIdAndStoreId(
                couponId, student.getId(), target.tourId(), storeId)) {
            throw alreadyReviewed();
        }

        try {
            return ReviewCreateResponse.of(reviewRepository.saveAndFlush(new StoreReview(
                    student.getId(), target.tourId(), storeId, couponId, rating, content)));
        } catch (DataIntegrityViolationException e) {
            // 같은 쿠폰으로 거의 동시에 두 번 쓴 경우 (쿠폰·사용자·탐방·상점 유니크)
            throw alreadyReviewed();
        }
    }

    // 상점의 AI 요약, 평균 별점, 리뷰 목록(최신순)
    @Transactional(readOnly = true)
    public ReviewListResponse list(Long storeId, Integer page, Integer size) {
        int pageNo = page == null ? 1 : page;
        int pageSize = size == null ? DEFAULT_SIZE : size;

        if (pageNo < 1) {
            throw StoreErrors.invalid("page", "page는 1 이상이어야 합니다");
        }
        if (pageSize < 1 || pageSize > MAX_SIZE) {
            throw StoreErrors.invalid("size", "size는 1~" + MAX_SIZE + " 사이여야 합니다");
        }

        requireStore(storeId);

        var rows = reviewRepository.findRows(storeId, PageRequest.of(pageNo - 1, pageSize));
        var stats = reviewRepository.stats(storeId);

        // 리뷰가 아직 없거나 요약이 만들어지기 전이면 null
        String summary = summaryRepository.findByStoreId(storeId)
                .map(StoreReviewSummary::getSummary)
                .orElse(null);
        double average = stats.roundedAverage();

        return new ReviewListResponse(
                summary,
                average,
                stats.count(),
                rows.stream().map(r -> new ReviewListResponse.Item(
                        r.reviewId(), r.rating(), r.content(),
                        maskName(r.authorName()), Kst.toOffset(r.createdAt()))).toList(),
                pageNo,
                pageSize,
                stats.count()
        );
    }

    // 이름 가운데를 가린다: 김하늘 → 김*늘, 김하 → 김*, 남궁하늘 → 남**늘
    static String maskName(String name) {
        if (name == null || name.isBlank()) {
            return "익명";
        }

        String trimmed = name.trim();
        int length = trimmed.codePointCount(0, trimmed.length());

        if (length == 1) {
            return "*";
        }

        int[] points = trimmed.codePoints().toArray();
        StringBuilder masked = new StringBuilder().appendCodePoint(points[0]);
        int stars = length == 2 ? 1 : length - 2;
        masked.append("*".repeat(stars));
        if (length > 2) {
            masked.appendCodePoint(points[length - 1]);
        }

        return masked.toString();
    }

    private void requireStore(Long storeId) {
        if (!storeRepository.existsById(storeId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "E404", "존재하지 않는 상점입니다");
        }
    }

    private Long requireCouponId(Long couponId) {
        if (couponId == null) {
            throw StoreErrors.invalid("coupon_id", "필수 값입니다");
        }

        return couponId;
    }

    private int requireRating(Number rating) {
        if (rating == null) {
            throw StoreErrors.invalid("rating", "필수 값입니다");
        }

        double value = rating.doubleValue();
        if (value != Math.rint(value)) {
            throw StoreErrors.invalid("rating", "정수여야 합니다");
        }
        if (value < StoreReview.RATING_MIN || value > StoreReview.RATING_MAX) {
            throw StoreErrors.invalid("rating",
                    StoreReview.RATING_MIN + "~" + StoreReview.RATING_MAX + " 사이여야 합니다");
        }

        return (int) value;
    }

    private String normalizeContent(String content) {
        String trimmed = content == null ? "" : content.trim();

        if (trimmed.isEmpty()) {
            throw StoreErrors.invalid("content", "필수 값입니다");
        }
        if (trimmed.length() > StoreReview.CONTENT_MAX) {
            throw StoreErrors.invalid("content", StoreReview.CONTENT_MAX + "자 이하로 입력해 주세요");
        }

        return trimmed;
    }

    private ApiException alreadyReviewed() {
        return new ApiException(
                HttpStatus.CONFLICT, "E409_ALREADY_REVIEWED", "이미 리뷰를 작성했습니다");
    }
}
