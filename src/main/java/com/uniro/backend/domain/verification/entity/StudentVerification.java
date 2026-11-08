package com.uniro.backend.domain.verification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "student_verifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentVerification {

    @Id
    private Long id;

    @Column(name = "high_school_name", length = 100)
    private String highSchoolName;

    @Column(name = "university_id")
    private Long universityId;

    @Column(name = "document_url", length = 500)
    private String documentUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VerificationStatus status = VerificationStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    public StudentVerification(
            Long userId,
            String highSchoolName,
            Long universityId,
            String documentUrl
    ) {
        this.id = userId;
        submit(highSchoolName, universityId, documentUrl);
    }

    public void resubmit(
            String highSchoolName,
            Long universityId,
            String documentUrl
    ) {
        if (status != VerificationStatus.REJECTED) {
            throw new IllegalStateException(
                    "반려된 인증만 다시 제출할 수 있습니다."
            );
        }

        submit(highSchoolName, universityId, documentUrl);
    }

    private void submit(
            String highSchoolName,
            Long universityId,
            String documentUrl
    ) {
        this.highSchoolName = highSchoolName;
        this.universityId = universityId;
        this.documentUrl = documentUrl;
        this.status = VerificationStatus.PENDING;
        this.rejectionReason = null;
        this.submittedAt = LocalDateTime.now();
        this.reviewedAt = null;
        this.reviewedBy = null;
    }

    public void review(
            VerificationStatus decision,
            String rejectReason,
            Long adminId,
            LocalDateTime reviewedAt
    ) {
        if (status != VerificationStatus.PENDING) {
            throw new IllegalStateException(
                    "이미 심사가 끝난 요청입니다."
            );
        }

        if (decision != VerificationStatus.APPROVED
                && decision != VerificationStatus.REJECTED) {
            throw new IllegalArgumentException(
                    "승인 또는 반려만 가능합니다."
            );
        }

        this.status = decision;
        this.rejectionReason =
                decision == VerificationStatus.REJECTED
                        ? rejectReason
                        : null;

        this.reviewedBy = adminId;
        this.reviewedAt = reviewedAt;

        // 심사 완료 후 기존 문서 위치를 DB에서 제거
        this.documentUrl = null;
    }
}