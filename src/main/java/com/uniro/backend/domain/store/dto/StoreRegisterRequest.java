package com.uniro.backend.domain.store.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

// store_id가 있으면 검색에서 고른 상점을 연결하고,
// 없으면 name, address, category, latitude, longitude로 직접 등록한다.
public record StoreRegisterRequest(
        @JsonProperty("store_id") Long storeId,
        String name,
        String address,
        String category,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
