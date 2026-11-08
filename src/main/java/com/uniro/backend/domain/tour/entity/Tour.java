package com.uniro.backend.domain.tour.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "tours")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "guide_id", nullable = false)
    private Long guideId;

    @Column(name = "university_id", nullable = false)
    private Long universityId;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "introduction", nullable = false, columnDefinition = "TEXT")
    private String introduction;

    @Column(name = "open_chat_url", length = 500)
    private String openChatUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TourStatus status = TourStatus.RECRUITING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = TourStatus.CANCELED;
    }

    public void complete() {
        this.status = TourStatus.COMPLETED;
    }

    // 모집 중이거나 정원이 차서 마감된 탐방이다 (취소·완료된 탐방은 아니다).
    // 이런 탐방만 신청 처리와 코스 작성이 가능하다.
    public boolean isOpen() {
        return status == TourStatus.RECRUITING || status == TourStatus.CLOSED;
    }

    // 정원이 차서 모집을 마감 (모집 중일 때만)
    public void close() {
        if (this.status == TourStatus.RECRUITING) {
            this.status = TourStatus.CLOSED;
        }
    }

    // 자리가 나서 모집을 다시 연다 (마감 상태일 때만. 취소·완료된 탐방은 바꾸지 않는다)
    public void reopen() {
        if (this.status == TourStatus.CLOSED) {
            this.status = TourStatus.RECRUITING;
        }
    }

    public Tour(
            Long guideId,
            Long universityId,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            Integer capacity,
            String introduction,
            String openChatUrl
    ) {
        this.guideId = guideId;
        this.universityId = universityId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.capacity = capacity;
        this.introduction = introduction;
        this.openChatUrl = openChatUrl;
        this.status = TourStatus.RECRUITING;
    }
}