package com.uniro.backend.domain.tour.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Getter
@Table(
        name = "tour_applications",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_tour_application_active_user",
                        columnNames = {"tour_id", "active_user_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TourApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tour_id", nullable = false)
    private Long tourId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "motivation", nullable = false, columnDefinition = "TEXT")
    private String motivation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TourApplicationStatus status = TourApplicationStatus.PENDING;

    @Column(name = "active_user_id")
    private Long activeUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 탐방 신청 (신청 중 상태로 시작한다)
    public TourApplication(Long tourId, Long userId, String motivation) {
        // DB 시각 정밀도와 응답이 어긋나지 않도록 초 단위로 맞춘다
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        this.tourId = tourId;
        this.userId = userId;
        this.motivation = motivation;
        this.status = TourApplicationStatus.PENDING;
        this.activeUserId = userId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void cancelByTour() {
        this.status = TourApplicationStatus.CANCELED;
        this.activeUserId = null;
        this.updatedAt = LocalDateTime.now();
    }

    // 가이드가 승인
    public void approve() {
        changeStatus(TourApplicationStatus.APPROVED);
    }

    // 가이드가 거절
    public void reject() {
        changeStatus(TourApplicationStatus.REJECTED);
    }

    // 신청한 고교생이 취소
    public void cancel() {
        changeStatus(TourApplicationStatus.CANCELED);
    }

    // 신청 중·승인 상태일 때만 유효 신청이다. 유효 신청은 사용자당 탐방 하나에 하나만 있을 수 있다
    // (tour_id, active_user_id 유니크). 거절·취소되면 active_user_id를 비워 다시 신청할 수 있게 한다.
    private void changeStatus(TourApplicationStatus next) {
        boolean active = next == TourApplicationStatus.PENDING
                || next == TourApplicationStatus.APPROVED;

        this.status = next;
        this.activeUserId = active ? userId : null;
        this.updatedAt = LocalDateTime.now();
    }
}