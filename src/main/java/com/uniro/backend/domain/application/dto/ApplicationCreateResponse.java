package com.uniro.backend.domain.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.uniro.backend.domain.application.support.Kst;
import com.uniro.backend.domain.tour.entity.TourApplication;
import com.uniro.backend.domain.tour.entity.TourApplicationStatus;

import java.time.OffsetDateTime;

public record ApplicationCreateResponse(
        @JsonProperty("application_id") Long applicationId,
        @JsonProperty("tour_id") Long tourId,
        TourApplicationStatus status,
        @JsonProperty("created_at") OffsetDateTime createdAt
) {

    public static ApplicationCreateResponse of(TourApplication application) {
        return new ApplicationCreateResponse(
                application.getId(),
                application.getTourId(),
                application.getStatus(),
                Kst.toOffset(application.getCreatedAt())
        );
    }
}
