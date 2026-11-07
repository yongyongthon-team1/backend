package com.uniro.backend.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.uniro.backend.domain.user.entity.User;
import com.uniro.backend.domain.user.entity.UserRole;
import com.uniro.backend.domain.verification.entity.VerificationStatus;

public record LoginResponse(

        @JsonProperty("access_token")
        String accessToken,

        UserInfo user
) {
    public record UserInfo(
            Long id,
            String name,
            UserRole role,

            @JsonProperty("verification_status")
            VerificationStatus verificationStatus
    ) {
    }

    public static LoginResponse from(
            String token,
            User user,
            VerificationStatus status
    ) {
        return new LoginResponse(
                token,
                new UserInfo(
                        user.getId(),
                        user.getName(),
                        user.getRole(),
                        status
                )
        );
    }
}