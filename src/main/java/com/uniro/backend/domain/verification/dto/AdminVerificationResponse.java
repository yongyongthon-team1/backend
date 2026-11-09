package com.uniro.backend.domain.verification.dto;

import java.util.List;

public record AdminVerificationResponse(
        List<Item> items,
        int page,
        int size,
        long total
) {
    public record Item(
            Long verification_id,
            String user_name,
            String role,
            String school_name,
            String submitted_at
    ) {
    }
}