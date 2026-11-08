package com.uniro.backend.domain.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.uniro.backend.domain.tour.entity.TourApplicationStatus;

public record ApplicationCancelResponse(
        @JsonProperty("application_id") Long applicationId,
        TourApplicationStatus status
) {
}
