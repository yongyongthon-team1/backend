package com.uniro.backend.domain.course.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 탐방 코스. 탐방당 하나이고, 탐방 ID가 곧 코스의 ID다.
@Entity
@Getter
@Table(name = "courses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course {

    @Id
    @Column(name = "tour_id")
    private Long tourId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CourseStatus status = CourseStatus.DRAFT;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    public boolean isConfirmed() {
        return status == CourseStatus.CONFIRMED;
    }
}
