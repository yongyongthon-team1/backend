package com.uniro.backend.domain.course.repository;

import com.uniro.backend.domain.course.entity.CourseStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseStopRepository extends JpaRepository<CourseStop, Long> {

    List<CourseStop> findByTourIdOrderByStopOrderAsc(Long tourId);

    // 지점을 통째로 바꿀 때 먼저 지운다. 바로 실행되므로, 새 지점을 넣을 때
    // (tour_id, stop_order) 유니크와 부딪히지 않는다.
    @Modifying(flushAutomatically = true)
    @Query("delete from CourseStop s where s.tourId = :tourId")
    void deleteAllByTourId(@Param("tourId") Long tourId);
}
