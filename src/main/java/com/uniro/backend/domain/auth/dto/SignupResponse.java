package com.uniro.backend.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.uniro.backend.domain.user.entity.User;
import com.uniro.backend.domain.user.entity.UserRole;

import java.time.ZoneId;
import java.time.OffsetDateTime;

public record SignupResponse(
        Long id,
        String email,
        String name,
        UserRole role,

        @JsonProperty("created_at")
        OffsetDateTime createdAt
) {
    public static SignupResponse from(User user) {
        return new SignupResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getCreatedAt()
                        .atZone(ZoneId.of("Asia/Seoul"))
                        .toOffsetDateTime()
        );
    }
}