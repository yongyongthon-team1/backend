package com.uniro.backend.domain.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// 코스 수정 요청. 보낸 지점 배열 전체가 새 코스가 되고, 배열 순서가 곧 방문 순서다.
public record CourseSpotsRequest(List<Spot> spots) {

    public record Spot(
            @JsonProperty("stop_type") String stopType,
            @JsonProperty("store_id") Long storeId,
            @JsonProperty("campus_building_id") Long campusBuildingId,
            @JsonProperty("start_time") String startTime,
            String comment
    ) {
    }
}
