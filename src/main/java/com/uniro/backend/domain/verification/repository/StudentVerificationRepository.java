package com.uniro.backend.domain.verification.repository;

import com.uniro.backend.domain.verification.entity.StudentVerification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentVerificationRepository
        extends JpaRepository<StudentVerification, Long> {
}