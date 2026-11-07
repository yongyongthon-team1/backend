package com.uniro.backend.domain.user.dto;

import com.uniro.backend.domain.user.entity.GuideProfile;

public record GuideProfileResponse(
        String major,
        String intro
) {
    public static GuideProfileResponse from(GuideProfile profile) {
        return new GuideProfileResponse(
                profile.getMajor(),
                profile.getIntroduction()
        );
    }
}