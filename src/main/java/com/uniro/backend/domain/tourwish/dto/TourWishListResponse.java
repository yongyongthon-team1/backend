package com.uniro.backend.domain.tourwish.dto;

import java.util.List;

public record TourWishListResponse(
        List<Item> items,
        int page,
        int size,
        long total
) {

    public record Item(
            Long wish_id,
            String campus_name,
            String major,
            String wish,
            String created_at
    ) {
    }
}