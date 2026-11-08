package com.uniro.backend.domain.tour.dto;

import java.util.List;

public record TourListResponse(
        List<Item> items,
        int page,
        int size,
        long total
) {

    public record Item(
            Long tour_id,
            String campus_name,
            String major,
            String date,
            String start_time,
            Integer capacity,
            Integer remaining,
            String status,
            Guide guide
    ) {
    }

    public record Guide(
            String name,
            boolean verified
    ) {
    }
}