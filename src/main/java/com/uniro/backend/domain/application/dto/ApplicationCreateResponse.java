package com.uniro.backend.domain.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.uniro.backend.domain.tour.entity.TourApplication;
import com.uniro.backend.domain.tour.entity.TourApplicationStatus;

import java.time.OffsetDateTime;
import java.time.ZoneId;

public record ApplicationCreateResponse(
        @JsonProperty("application_id") Long applicationId,
        @JsonProperty("tour_id") Long tourId,
        TourApplicationStatus status,
        @JsonProperty("created_at") OffsetDateTime createdAt
) {
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    public static ApplicationCreateResponse of(TourApplication application) {
        return new ApplicationCreateResponse(
                application.getId(),
                application.getTourId(),
                application.getStatus(),
                application.getCreatedAt().atZone(ZONE).toOffsetDateTime()
        );
    }
}
