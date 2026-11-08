package com.uniro.backend.domain.verification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "business_verifications")
public class BusinessVerification {

    @Id
    private Long id;

    @Column(name = "is_verified", nullable = false)
    private boolean verified;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public BusinessVerification(Long userId, LocalDateTime now) {
        this.id = userId;
        approve(now);
    }

    public void approve(LocalDateTime now) {
        this.verified = true;
        this.verifiedAt = now;
        this.updatedAt = now;
    }
}