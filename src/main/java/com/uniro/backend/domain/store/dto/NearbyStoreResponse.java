package com.uniro.backend.domain.store.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.uniro.backend.domain.store.entity.Store;

import java.math.BigDecimal;

// 주변 상점 조회 응답의 상점 하나.
// review_summary는 리뷰 요약 기능이 생기기 전까지 항상 null이다.
public record NearbyStoreResponse(
        @JsonProperty("store_id") Long storeId,
        String name,
        String category,
        String address,
        BigDecimal lat,
        BigDecimal lng,
        @JsonProperty("is_partner") boolean isPartner,
        @JsonProperty("review_summary") String reviewSummary
) {
    public static NearbyStoreResponse of(Store store, boolean partner) {
        return new NearbyStoreResponse(
                store.getId(),
                store.getName(),
                store.getCategory(),
                store.getAddress(),
                store.getLatitude(),
                store.getLongitude(),
                partner,
                null
        );
    }
}
