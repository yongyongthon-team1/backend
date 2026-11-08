package com.uniro.backend.domain.verification.repository;

import com.uniro.backend.domain.verification.entity.StudentVerification;
import com.uniro.backend.domain.verification.entity.VerificationStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StudentVerificationRepository
        extends JpaRepository<StudentVerification, Long> {

    Page<StudentVerification> findByStatus(
            VerificationStatus status,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM StudentVerification v WHERE v.id = :id")
    Optional<StudentVerification> findByIdForUpdate(
            @Param("id") Long id
    );
}