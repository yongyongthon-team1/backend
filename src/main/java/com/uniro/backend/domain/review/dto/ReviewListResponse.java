package com.uniro.backend.domain.review.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;

public record ReviewListResponse(
        String summary,
        @JsonProperty("average_rating") double averageRating,
        @JsonProperty("review_count") long reviewCount,
        List<Item> items,
        int page,
        int size,
        long total
) {
    public record Item(
            @JsonProperty("review_id") Long reviewId,
            Integer rating,
            String content,
            String author,
            @JsonProperty("created_at") OffsetDateTime createdAt
    ) {
    }
}
