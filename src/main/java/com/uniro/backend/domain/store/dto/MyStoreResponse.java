package com.uniro.backend.domain.store.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

// 내 가게 조회·수정 응답. 제휴 할인이 없으면 partnership은 null이다.
public record MyStoreResponse(
        @JsonProperty("store_id") Long storeId,
        String name,
        String address,
        String category,
        Partnership partnership
) {
    public record Partnership(
            @JsonProperty("discount_text") String discountText,
            @JsonProperty("start_date") LocalDate startDate,
            @JsonProperty("end_date") LocalDate endDate
    ) {
    }
}
