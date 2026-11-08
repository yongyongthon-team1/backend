package com.uniro.backend.domain.review.repository;

import com.uniro.backend.domain.review.entity.StoreReview;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface StoreReviewRepository extends JpaRepository<StoreReview, Long> {

    // 이 쿠폰이나, 같은 탐방의 같은 상점에 이미 쓴 리뷰가 있는지
    boolean existsByCouponIdOrUserIdAndTourIdAndStoreId(
            Long couponId, Long userId, Long tourId, Long storeId);

    // 주어진 쿠폰 중 이미 리뷰를 쓴 쿠폰 ID
    @Query("select r.couponId from StoreReview r where r.couponId in :couponIds")
    List<Long> findReviewedCouponIds(@Param("couponIds") Collection<Long> couponIds);

    // 상점의 리뷰 (최신순) + 작성자 이름. 작성자 행이 없어도 리뷰는 목록에 남는다.
    @Query("""
            select new com.uniro.backend.domain.review.repository.StoreReviewRepository$ReviewRow(
                r.id, r.rating, r.content, u.name, r.createdAt)
            from StoreReview r
            left join User u on u.id = r.userId
            where r.storeId = :storeId
            order by r.createdAt desc, r.id desc
            """)
    List<ReviewRow> findRows(@Param("storeId") Long storeId, Pageable pageable);

    // 상점의 리뷰 수와 평균 별점 (리뷰가 없으면 평균은 null)
    @Query("""
            select new com.uniro.backend.domain.review.repository.StoreReviewRepository$ReviewStats(
                count(r), avg(r.rating))
            from StoreReview r
            where r.storeId = :storeId
            """)
    ReviewStats stats(@Param("storeId") Long storeId);

    // 기간 안(from 포함, to 미포함)에 쓴 상점의 리뷰 수와 평균 별점
    @Query("""
            select new com.uniro.backend.domain.review.repository.StoreReviewRepository$ReviewStats(
                count(r), avg(r.rating))
            from StoreReview r
            where r.storeId = :storeId
              and r.createdAt >= :from
              and r.createdAt < :to
            """)
    ReviewStats statsBetween(
            @Param("storeId") Long storeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    record ReviewStats(long count, Double average) {

        // 소수 첫째 자리까지 반올림한 평균 별점. 리뷰가 없으면 0.0.
        public double roundedAverage() {
            return average == null ? 0.0 : Math.round(average * 10) / 10.0;
        }
    }

    record ReviewRow(
            Long reviewId, Integer rating, String content, String authorName,
            LocalDateTime createdAt
    ) {
    }
}
