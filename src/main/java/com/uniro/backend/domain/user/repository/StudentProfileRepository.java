package com.uniro.backend.domain.user.repository;

import com.uniro.backend.domain.user.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentProfileRepository
        extends JpaRepository<StudentProfile, Long> {
}