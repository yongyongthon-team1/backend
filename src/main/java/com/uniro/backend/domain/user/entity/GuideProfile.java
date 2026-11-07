package com.uniro.backend.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "guide_profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuideProfile {

    @Id
    private Long id;

    @Column(name = "university_id")
    private Long universityId;

    @Column(name = "major", length = 100)
    private String major;

    @Column(name = "introduction", columnDefinition = "TEXT")
    private String introduction;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 최초 가이드 프로필 생성
    public GuideProfile(Long userId, String major, String introduction) {
        this.id = userId;
        this.major = major;
        this.introduction = introduction;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 기존 가이드 프로필 수정
    public void updateProfile(String major, String introduction) {
        this.major = major;
        this.introduction = introduction;
        this.updatedAt = LocalDateTime.now();
    }
}