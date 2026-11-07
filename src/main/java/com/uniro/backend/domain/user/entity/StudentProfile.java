package com.uniro.backend.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "student_profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentProfile {

    @Id
    private Long id;

    @Column(name = "high_school_name", length = 100)
    private String highSchoolName;

    @Column(name = "grade")
    private Integer grade;

    @Column(name = "interested_major", length = 100)
    private String interestedMajor;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public StudentProfile(Long userId) {
        this.id = userId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateHighSchoolName(String highSchoolName) {
        this.highSchoolName = highSchoolName;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateGrade(Integer grade) {
        this.grade = grade;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateInterestedMajor(String interestedMajor) {
        this.interestedMajor = interestedMajor;
        this.updatedAt = LocalDateTime.now();
    }
}