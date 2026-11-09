package com.uniro.backend.domain.campus.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record CampusPlaceResponse(
        @JsonProperty("campus_building_id") Long campusBuildingId,
        String name,
        String description,
        BigDecimal lat,
        BigDecimal lng
) {
}
