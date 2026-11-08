package com.uniro.backend.domain.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.uniro.backend.domain.course.entity.CourseStatus;
import com.uniro.backend.domain.course.entity.StopType;

import java.math.BigDecimal;
import java.util.List;

// 코스 조회·수정 응답. 지점(spots)의 형식은 모든 코스 API가 같다.
// STORE 지점은 상점 필드를, CAMPUS 지점은 캠퍼스 건물 필드를 채우고 나머지는 null이다.
public record CourseResponse(
        @JsonProperty("tour_id") Long tourId,
        CourseStatus status,
        List<Spot> spots
) {
    public record Spot(
            int order,
            @JsonProperty("stop_type") StopType stopType,
            @JsonProperty("store_id") Long storeId,
            @JsonProperty("campus_building_id") Long campusBuildingId,
            String name,
            String category,
            String address,
            BigDecimal lat,
            BigDecimal lng,
            @JsonProperty("start_time") String startTime,
            String reason,
            String comment,
            @JsonProperty("is_partner") boolean isPartner,
            @JsonProperty("review_summary") String reviewSummary
    ) {
    }
}
