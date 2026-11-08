package com.uniro.backend.domain.store.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StoreSearchItemResponse(
        @JsonProperty("store_id") Long storeId,
        String name,
        String address,
        String category,
        @JsonProperty("is_registered") boolean isRegistered
) {
}
