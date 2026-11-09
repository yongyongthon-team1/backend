package com.uniro.backend.domain.review.event;

// 리뷰가 저장(커밋)됐다. 그 상점의 AI 리뷰 요약을 갱신할지 정하는 데 쓴다.
public record ReviewCreatedEvent(Long storeId) {
}
