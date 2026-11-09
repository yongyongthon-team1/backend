package com.uniro.backend.domain.review.entity;

import com.uniro.backend.common.Kst;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 상점 리뷰. 쿠폰 한 장에 리뷰 하나, 같은 탐방에서는 같은 상점에 한 번만 쓸 수 있다.
@Entity
@Getter
@Table(
        name = "store_reviews",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_store_reviews_coupon", columnNames = "coupon_id"),
                @UniqueConstraint(
                        name = "uk_store_reviews_user_tour_store",
                        columnNames = {"user_id", "tour_id", "store_id"}
                )
        },
        indexes = @Index(name = "idx_store_reviews_store_created", columnList = "store_id, created_at")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreReview {

    public static final int CONTENT_MAX = 100;
    public static final int RATING_MIN = 1;
    public static final int RATING_MAX = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "tour_id", nullable = false)
    private Long tourId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Column(name = "rating", nullable = false, columnDefinition = "TINYINT")
    private Integer rating;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public StoreReview(
            Long userId, Long tourId, Long storeId, Long couponId, Integer rating, String content
    ) {
        if (rating == null || rating < RATING_MIN || rating > RATING_MAX) {
            throw new IllegalArgumentException("별점은 " + RATING_MIN + "~" + RATING_MAX + " 사이여야 합니다");
        }

        LocalDateTime now = Kst.nowToSeconds();

        this.userId = userId;
        this.tourId = tourId;
        this.storeId = storeId;
        this.couponId = couponId;
        this.rating = rating;
        this.content = content;
        this.createdAt = now;
        this.updatedAt = now;
    }
}
