package com.uniro.backend.domain.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.uniro.backend.domain.course.entity.CourseStatus;

public record CourseConfirmResponse(
        @JsonProperty("tour_id") Long tourId,
        CourseStatus status,
        @JsonProperty("coupon_count") int couponCount
) {
}
