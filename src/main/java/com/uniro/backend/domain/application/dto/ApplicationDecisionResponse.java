package com.uniro.backend.domain.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.uniro.backend.domain.tour.entity.TourApplicationStatus;
import com.uniro.backend.domain.tour.entity.TourStatus;

public record ApplicationDecisionResponse(
        @JsonProperty("application_id") Long applicationId,
        TourApplicationStatus status,
        @JsonProperty("tour_status") TourStatus tourStatus
) {
}
