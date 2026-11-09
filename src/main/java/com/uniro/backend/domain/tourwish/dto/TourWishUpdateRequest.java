package com.uniro.backend.domain.tourwish.dto;

import java.time.LocalDate;

public record TourWishUpdateRequest(
        Long campusId,
        String major,
        String wish,
        LocalDate date,
        boolean updateDate
) {
}