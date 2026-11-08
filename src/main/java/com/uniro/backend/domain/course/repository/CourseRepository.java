package com.uniro.backend.domain.course.repository;

import com.uniro.backend.domain.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    // 탐방의 가이드 ID. 탐방 행을 잠그기 전에 권한만 확인하려고, 엔티티를 읽지 않고 값만 가져온다
    // (엔티티를 먼저 읽어 두면 잠근 뒤에도 그때의 오래된 상태가 남는다).
    @Query("select t.guideId from Tour t where t.id = :tourId")
    Optional<Long> findGuideIdByTourId(@Param("tourId") Long tourId);
}
