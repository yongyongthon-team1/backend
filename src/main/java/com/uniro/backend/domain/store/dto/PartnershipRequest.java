package com.uniro.backend.domain.store.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record PartnershipRequest(
        @JsonProperty("discount_text") String discountText,
        @JsonProperty("start_date") LocalDate startDate,
        @JsonProperty("end_date") LocalDate endDate
) {
}
