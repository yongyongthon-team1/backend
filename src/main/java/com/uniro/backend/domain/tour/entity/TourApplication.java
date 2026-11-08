package com.uniro.backend.domain.tour.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    public void cancelByTour() {
        this.status = TourApplicationStatus.CANCELED;
        this.activeUserId = null;
        this.updatedAt = LocalDateTime.now();
    }
}