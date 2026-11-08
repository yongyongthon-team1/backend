package com.uniro.backend.domain.tour.dto;

public record TourDetailResponse(
        Long tour_id,
        String campus_name,
        String major,
        String date,
        String start_time,
        String end_time,
        Integer capacity,
        Integer remaining,
        String status,
        String intro,
        String open_chat_url,
        Guide guide,
        MyApplication my_application
) {

    public record Guide(
            String name,
            String major,
            String intro,
            boolean verified
    ) {
    }

    public record MyApplication(
            Long application_id,
            String status
    ) {
    }
}