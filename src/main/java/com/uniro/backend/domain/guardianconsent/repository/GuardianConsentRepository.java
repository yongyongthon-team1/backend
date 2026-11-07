package com.uniro.backend.domain.guardianconsent.repository;

import com.uniro.backend.domain.guardianconsent.entity.GuardianConsent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuardianConsentRepository
        extends JpaRepository<GuardianConsent, Long> {
}