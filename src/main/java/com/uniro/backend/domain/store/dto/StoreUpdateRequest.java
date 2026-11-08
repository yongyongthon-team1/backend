package com.uniro.backend.domain.store.dto;

// 보낸 필드만 수정한다
public record StoreUpdateRequest(
        String name,
        String address,
        String category
) {
}
