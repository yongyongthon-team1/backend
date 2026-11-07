package com.uniro.backend.domain.verification.repository;

import com.uniro.backend.domain.verification.entity.BusinessVerification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessVerificationRepository
        extends JpaRepository<BusinessVerification, Long> {
}