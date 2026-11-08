package com.uniro.backend.domain.store.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

// 사장님의 방문·리뷰 리포트: 기간 안에 쿠폰이 사용된 횟수와 리뷰 수·평균 별점
public record StoreReportResponse(
        LocalDate from,
        LocalDate to,
        @JsonProperty("visit_count") long visitCount,
        @JsonProperty("review_count") long reviewCount,
        @JsonProperty("average_rating") double averageRating
) {
}
