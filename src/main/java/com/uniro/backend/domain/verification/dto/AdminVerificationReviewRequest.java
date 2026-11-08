package com.uniro.backend.domain.verification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AdminVerificationReviewRequest(
        String decision,

        @JsonProperty("reject_reason")
        String rejectReason
) {
}