package com.uniro.backend.domain.campus.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CampusResponse(
        @JsonProperty("campus_id") Long campusId,
        String name,
        @JsonProperty("campus_name") String campusName,
        String address,
        BigDecimal lat,
        BigDecimal lng,
        @JsonProperty("vacation_periods") List<VacationPeriod> vacationPeriods
) {
    public record VacationPeriod(
            @JsonProperty("start_date") LocalDate startDate,
            @JsonProperty("end_date") LocalDate endDate
    ) {
    }
}
