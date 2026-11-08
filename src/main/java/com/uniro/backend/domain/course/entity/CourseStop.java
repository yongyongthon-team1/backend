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

    // 확정할 때의 할인 내용. 이후 제휴 내용이 바뀌어도, 코스가 확정된 뒤에 승인된 신청자까지
    // 모두 같은 내용의 쿠폰을 받도록 남겨 둔다.
    @Column(name = "partnership_discount")
    private String partnershipDiscount;

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

    private CourseStop(Long tourId, int order, LocalTime visitTime, String description) {
        this.tourId = tourId;
        this.stopOrder = order;
        this.visitTime = visitTime;
        this.description = description;
    }

    // 상점 지점. reason은 같은 상점을 유지했을 때 이어받는 AI 추천 이유(없으면 null).
    public static CourseStop ofStore(
            Long tourId, int order, Long storeId, LocalTime visitTime,
            String description, String reason
    ) {
        CourseStop stop = new CourseStop(tourId, order, visitTime, description);
        stop.stopType = StopType.STORE;
        stop.storeId = storeId;
        stop.recommendationReason = reason;
        return stop;
    }

    // 코스를 확정할 때, 제휴 기간 안인 상점 지점에 제휴 행사를 기록한다
    public void assignPartnership(Long partnershipId, String discount) {
        this.partnershipId = partnershipId;
        this.partnershipDiscount = discount;
    }

    // 캠퍼스 건물 지점. AI 추천 이유는 없다.
    public static CourseStop ofCampus(
            Long tourId, int order, Long campusBuildingId, LocalTime visitTime, String description
    ) {
        CourseStop stop = new CourseStop(tourId, order, visitTime, description);
        stop.stopType = StopType.CAMPUS;
        stop.campusBuildingId = campusBuildingId;
        return stop;
    }
}
