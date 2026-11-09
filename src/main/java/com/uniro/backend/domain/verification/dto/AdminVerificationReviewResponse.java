package com.uniro.backend.domain.verification.dto;

public record AdminVerificationReviewResponse(
        Long verification_id,
        String status,
        String reviewed_at
) {
}