package com.uniro.backend.domain.tourwish.dto;

public record TourWishUpdateResponse(
        Long wish_id,
        String campus_name,
        String major,
        String wish,
        String date,
        String updated_at
) {
}