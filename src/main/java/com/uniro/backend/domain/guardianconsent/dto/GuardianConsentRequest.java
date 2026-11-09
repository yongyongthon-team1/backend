package com.uniro.backend.domain.guardianconsent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GuardianConsentRequest(
        @JsonProperty("guardian_name")
        String guardianName,

        @JsonProperty("guardian_email")
        String guardianEmail
) {
}