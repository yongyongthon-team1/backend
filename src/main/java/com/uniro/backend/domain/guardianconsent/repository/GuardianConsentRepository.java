package com.uniro.backend.domain.guardianconsent.repository;

import com.uniro.backend.domain.guardianconsent.entity.GuardianConsent;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GuardianConsentRepository
        extends JpaRepository<GuardianConsent, Long> {

    Optional<GuardianConsent> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select g
            from GuardianConsent g
            where g.tokenHash = :tokenHash
            """)
    Optional<GuardianConsent> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );
}