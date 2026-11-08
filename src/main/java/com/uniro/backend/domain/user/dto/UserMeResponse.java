package com.uniro.backend.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.uniro.backend.domain.user.entity.UserRole;

public record UserMeResponse(
        Long id,
        String email,
        String name,
        UserRole role,

        @JsonProperty("verification_status")
        String verificationStatus,

        @JsonProperty("reject_reason")
        String rejectReason,

        @JsonProperty("school_name")
        String schoolName,

        @JsonProperty("university_id")
        Long universityId,

        Integer grade,

        @JsonProperty("interest_major")
        String interestMajor,

        @JsonProperty("guardian_consent_status")
        String guardianConsentStatus,

        @JsonProperty("guide_profile")
        GuideProfileResponse guideProfile
) {
}