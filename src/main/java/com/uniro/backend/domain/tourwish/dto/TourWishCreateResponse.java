package com.uniro.backend.domain.tourwish.dto;

public record TourWishCreateResponse(
        Long wish_id,
        Long campus_id,
        String major,
        String wish,
        String date,
        String created_at
) {
}