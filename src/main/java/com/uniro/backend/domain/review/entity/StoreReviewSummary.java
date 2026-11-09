package com.uniro.backend.domain.review.entity;

import com.uniro.backend.common.Kst;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 상점 리뷰 AI 요약 (상점당 하나). 리뷰가 충분히 쌓이면 ReviewSummaryService가 만들고 다시 만든다.
@Entity
@Getter
@Table(
        name = "store_review_summaries",
        uniqueConstraints = @UniqueConstraint(name = "uk_review_summaries_store", columnNames = "store_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreReviewSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "summary", nullable = false, columnDefinition = "TEXT")
    private String summary;

    // 요약을 만들 때 반영한 리뷰 수
    @Column(name = "review_count", nullable = false)
    private Integer reviewCount;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    public StoreReviewSummary(Long storeId, String summary, int reviewCount) {
        this.storeId = storeId;
        refresh(summary, reviewCount);
    }

    // 새로 만든 요약으로 바꾼다
    public void refresh(String summary, int reviewCount) {
        this.summary = summary;
        this.reviewCount = reviewCount;
        this.generatedAt = Kst.nowToSeconds();
    }
}
