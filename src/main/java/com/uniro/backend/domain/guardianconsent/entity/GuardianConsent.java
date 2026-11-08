package com.uniro.backend.domain.guardianconsent.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "guardian_consents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuardianConsent {

    @Id
    private Long id;

    @Column(name = "guardian_name", nullable = false, length = 100)
    private String guardianName;

    @Column(name = "guardian_email", nullable = false, length = 255)
    private String guardianEmail;

    @Column(name = "token_hash", length = 255)
    private String tokenHash;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "agreed_at")
    private LocalDateTime agreedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 최초 보호자 동의 요청
    public GuardianConsent(
            Long userId,
            String guardianName,
            String guardianEmail,
            String tokenHash,
            LocalDateTime now
    ) {
        this.id = userId;
        this.createdAt = now;

        requestConsent(
                guardianName,
                guardianEmail,
                tokenHash,
                now
        );
    }

    // 최초 요청 또는 재요청 시 사용
    public void requestConsent(
            String guardianName,
            String guardianEmail,
            String tokenHash,
            LocalDateTime now
    ) {
        this.guardianName = guardianName;
        this.guardianEmail = guardianEmail;
        this.tokenHash = tokenHash;
        this.status = "PENDING";
        this.expiresAt = now.plusDays(7);
        this.agreedAt = null;
        this.updatedAt = now;
    }

    // 보호자 동의 또는 거부 처리
    public void respond(boolean agree, LocalDateTime now) {
        this.status = agree ? "AGREED" : "REJECTED";
        this.agreedAt = agree ? now : null;
        this.updatedAt = now;
    }
}