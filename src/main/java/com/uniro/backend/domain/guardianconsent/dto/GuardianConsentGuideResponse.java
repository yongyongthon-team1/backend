package com.uniro.backend.domain.guardianconsent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record GuardianConsentGuideResponse(

        @JsonProperty("student_name")
        String studentName,

        @JsonProperty("school_name")
        String schoolName,

        @JsonProperty("guide_text")
        String guideText,

        String status,

        @JsonProperty("expires_at")
        OffsetDateTime expiresAt
) {
}