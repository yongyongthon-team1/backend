package com.uniro.backend.domain.tour.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalTime;

public record TourCreateRequest(
        LocalDate date,

        @JsonProperty("start_time")
        LocalTime startTime,

        @JsonProperty("end_time")
        LocalTime endTime,

        Integer capacity,

        String intro,

        @JsonProperty("open_chat_url")
        String openChatUrl
) {
}