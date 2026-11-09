package com.uniro.backend.domain.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.uniro.backend.common.Kst;
import com.uniro.backend.domain.tour.entity.TourApplicationStatus;
import com.uniro.backend.domain.tour.repository.TourApplicationRepository.ApplicantRow;

import java.time.OffsetDateTime;
import java.util.List;

public record ApplicantListResponse(List<Item> items) {

    public static ApplicantListResponse of(List<ApplicantRow> rows) {
        return new ApplicantListResponse(rows.stream().map(Item::of).toList());
    }

    public record Item(
            @JsonProperty("application_id") Long applicationId,
            String name,
            Integer grade,
            String wish,
            TourApplicationStatus status,
            @JsonProperty("created_at") OffsetDateTime createdAt
    ) {
        static Item of(ApplicantRow row) {
            return new Item(
                    row.applicationId(),
                    row.name(),
                    row.grade(),
                    row.wish(),
                    row.status(),
                    Kst.toOffset(row.createdAt())
            );
        }
    }
}
