package com.uniro.backend.domain.course.entity;

import com.uniro.backend.common.Kst;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;

// 탐방 코스. 탐방당 하나이고, 탐방 ID가 곧 코스의 ID다.
@Entity
@Getter
@Table(name = "courses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course implements Persistable<Long> {

    @Id
    @Column(name = "tour_id")
    private Long tourId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CourseStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    // 새 코스는 초안으로 시작한다
    public Course(Long tourId) {
        LocalDateTime now = Kst.nowToSeconds();

        this.tourId = tourId;
        this.isNew = true;
        this.status = CourseStatus.DRAFT;
        this.createdAt = now;
        this.updatedAt = now;
    }

    // 지점을 바꿨을 때 수정 시각을 갱신한다
    public void touch() {
        this.updatedAt = Kst.nowToSeconds();
    }

    // 탐방 ID를 직접 PK로 쓰므로 새 코스인지 알려 주어야, 저장할 때 헛된 SELECT를 하지 않는다
    @Transient
    private boolean isNew;

    @Override
    public Long getId() {
        return tourId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    // 확정한다. 한 번 확정하면 되돌릴 수 없다.
    public void confirm() {
        LocalDateTime now = Kst.nowToSeconds();

        this.status = CourseStatus.CONFIRMED;
        this.confirmedAt = now;
        this.updatedAt = now;
    }

    public boolean isConfirmed() {
        return status == CourseStatus.CONFIRMED;
    }
}
