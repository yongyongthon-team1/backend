package com.uniro.backend.domain.course.repository;

import com.uniro.backend.domain.course.entity.Course;
import com.uniro.backend.domain.course.entity.CourseStatus;
import com.uniro.backend.domain.tour.entity.TourStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    // 탐방의 가이드 ID. 탐방 행을 잠그기 전에 권한만 확인하려고, 엔티티를 읽지 않고 값만 가져온다
    // (엔티티를 먼저 읽어 두면 잠근 뒤에도 그때의 오래된 상태가 남는다).
    @Query("select t.guideId from Tour t where t.id = :tourId")
    Optional<Long> findGuideIdByTourId(@Param("tourId") Long tourId);

    // 코스 초안을 만들기 전에 탐방 행을 잠그지 않고 확인할 값들 (엔티티를 읽어 두지 않는다)
    @Query("""
            select new com.uniro.backend.domain.course.repository.CourseRepository$TourInfo(
                t.guideId, t.universityId, t.startsAt, t.status)
            from Tour t where t.id = :tourId
            """)
    Optional<TourInfo> findTourInfo(@Param("tourId") Long tourId);

    record TourInfo(
            Long guideId, Long universityId, LocalDateTime startsAt, TourStatus status
    ) {
    }

    boolean existsByTourIdAndStatus(Long tourId, CourseStatus status);
}
