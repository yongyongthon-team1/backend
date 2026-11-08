package com.uniro.backend.domain.review.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.uniro.backend.common.Kst;
import com.uniro.backend.domain.review.entity.StoreReview;

import java.time.OffsetDateTime;

public record ReviewCreateResponse(
        @JsonProperty("review_id") Long reviewId,
        @JsonProperty("store_id") Long storeId,
        Integer rating,
        String content,
        @JsonProperty("created_at") OffsetDateTime createdAt
) {
    public static ReviewCreateResponse of(StoreReview review) {
        return new ReviewCreateResponse(
                review.getId(),
                review.getStoreId(),
                review.getRating(),
                review.getContent(),
                Kst.toOffset(review.getCreatedAt())
        );
    }
}
