package com.uniro.backend.domain.verification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BusinessVerificationRequest(

        @JsonProperty("business_number")
        String businessNumber,

        @JsonProperty("representative_name")
        String representativeName,

        @JsonProperty("opened_at")
        String openedAt
) {
}