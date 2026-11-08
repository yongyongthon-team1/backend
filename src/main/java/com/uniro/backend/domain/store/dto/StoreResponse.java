package com.uniro.backend.domain.store.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// 가게 등록 응답
public record StoreResponse(
        @JsonProperty("store_id") Long storeId,
        String name,
        String address,
        String category
) {
}
