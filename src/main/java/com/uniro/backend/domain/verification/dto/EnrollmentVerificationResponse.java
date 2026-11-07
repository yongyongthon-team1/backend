package com.uniro.backend.domain.verification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.uniro.backend.domain.verification.entity.VerificationStatus;

import java.time.OffsetDateTime;

public record EnrollmentVerificationResponse(

        @JsonProperty("verification_id")
        Long verificationId,

        VerificationStatus status,

        @JsonProperty("submitted_at")
        OffsetDateTime submittedAt
) {
}