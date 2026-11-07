package com.uniro.backend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuideProfileRequest(

        @NotBlank(message = "학과는 필수입니다.")
        @Size(max = 30, message = "학과는 30자 이내여야 합니다.")
        String major,

        @NotBlank(message = "소개는 필수입니다.")
        @Size(max = 200, message = "소개는 200자 이내여야 합니다.")
        String intro

) {
}