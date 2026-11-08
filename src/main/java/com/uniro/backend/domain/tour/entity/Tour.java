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