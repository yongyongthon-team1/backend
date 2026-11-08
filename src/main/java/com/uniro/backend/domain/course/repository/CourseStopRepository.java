package com.uniro.backend.domain.course.repository;

import com.uniro.backend.domain.course.entity.CourseStop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseStopRepository extends JpaRepository<CourseStop, Long> {

    List<CourseStop> findByTourIdOrderByStopOrderAsc(Long tourId);
}
