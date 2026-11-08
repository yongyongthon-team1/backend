package com.uniro.backend.domain.guardianconsent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record GuardianConsentResponse(
        String status,

        @JsonProperty("expires_at")
        OffsetDateTime expiresAt
) {
}