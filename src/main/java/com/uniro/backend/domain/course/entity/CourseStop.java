package com.uniro.backend.domain.course.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

// 코스 지점. 상점(STORE) 또는 캠퍼스 건물(CAMPUS) 하나를 방문 순서대로 가리킨다.
@Entity
@Getter
@Table(
        name = "course_stops",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_course_stops_tour_order",
                columnNames = {"tour_id", "stop_order"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tour_id", nullable = false)
    private Long tourId;

    @Enumerated(EnumType.STRING)
    @Column(name = "stop_type", nullable = false, length = 20)
    private StopType stopType;

    @Column(name = "campus_building_id")
    private Long campusBuildingId;

    @Column(name = "store_id")
    private Long storeId;

    // 코스를 확정할 때 그 상점의 제휴 행사를 기록한다. 제휴 상점이 아니면 null.
    @Column(name = "partnership_id")
    private Long partnershipId;

    @Column(name = "stop_order", nullable = false)
    private Integer stopOrder;

    @Column(name = "visit_time")
    private LocalTime visitTime;

    // 가이드 한줄평
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // AI 추천 이유
    @Column(name = "recommendation_reason", columnDefinition = "TEXT")
    private String recommendationReason;
}
