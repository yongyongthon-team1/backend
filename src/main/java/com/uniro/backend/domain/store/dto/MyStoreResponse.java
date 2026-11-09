package com.uniro.backend.domain.store.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.uniro.backend.domain.store.entity.StorePartnership;

import java.time.LocalDate;

// 내 가게 조회·수정 응답. 제휴 할인이 없으면 partnership은 null이다.
public record MyStoreResponse(
        @JsonProperty("store_id") Long storeId,
        String name,
        String address,
        String category,
        Partnership partnership
) {
    // 제휴 할인 설정 응답과 내 가게 조회의 partnership이 같은 모양이다
    public record Partnership(
            @JsonProperty("discount_text") String discountText,
            @JsonProperty("start_date") LocalDate startDate,
            @JsonProperty("end_date") LocalDate endDate
    ) {
        public static Partnership from(StorePartnership p) {
            return new Partnership(
                    p.getDiscountDescription(),
                    p.getStartDate(),
                    p.getEndDate()
            );
        }
    }
}
