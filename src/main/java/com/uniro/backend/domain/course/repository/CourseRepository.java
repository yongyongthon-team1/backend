package com.uniro.backend.domain.course.repository;

import com.uniro.backend.domain.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
}
