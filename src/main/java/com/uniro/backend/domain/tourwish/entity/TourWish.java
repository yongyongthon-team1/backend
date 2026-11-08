package com.uniro.backend.domain.tourwish.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "tour_wishes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TourWish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "university_id", nullable = false)
    private Long universityId;

    @Column(name = "major", nullable = false, length = 100)
    private String major;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "preferred_date")
    private LocalDate preferredDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public TourWish(
            Long userId,
            Long universityId,
            String major,
            String content,
            LocalDate preferredDate
    ) {
        this.userId = userId;
        this.universityId = universityId;
        this.major = major;
        this.content = content;
        this.preferredDate = preferredDate;
    }

    public void update(
            Long universityId,
            String major,
            String content,
            LocalDate preferredDate,
            boolean updateDate
    ) {
        if (universityId != null) {
            this.universityId = universityId;
        }

        if (major != null) {
            this.major = major;
        }

        if (content != null) {
            this.content = content;
        }

        if (updateDate) {
            this.preferredDate = preferredDate;
        }

        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}