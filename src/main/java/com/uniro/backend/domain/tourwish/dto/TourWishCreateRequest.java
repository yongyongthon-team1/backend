package com.uniro.backend.domain.tourwish.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

public record TourWishCreateRequest(

        @JsonProperty("campus_id")
        Long campusId,

        String major,

        String wish,

        LocalDate date
) {
}