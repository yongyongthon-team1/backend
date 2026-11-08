package com.uniro.backend.domain.verification.dto;

public record AdminVerificationDetailResponse(
        Long verification_id,
        String user_name,
        String role,
        String school_name,
        String document_url,
        String status,
        String submitted_at
) {
}