package com.uniro.backend.domain.tour.entity;

public enum TourStatus {
    RECRUITING,
    CLOSED,
    CANCELED,
    COMPLETED;

    // 모집 중이거나 정원이 차서 마감된 상태다 (취소·완료는 아니다)
    public boolean isOpen() {
        return this == RECRUITING || this == CLOSED;
    }
}