package com.uniro.backend.domain.tourwish.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TourWishUpdateSwaggerRequest(

        @Schema(description = "캠퍼스 ID", example = "1")
        Long campus_id,

        @Schema(description = "희망 학과 (30자 이내)", example = "컴퓨터공학")
        String major,

        @Schema(
                description = "희망 사항 (100자 이내)",
                example = "게임 동아리와 학식을 체험하고 싶어요"
        )
        String wish,

        @Schema(
                description = "희망 날짜 (yyyy-MM-dd, null 입력 시 삭제)",
                example = "2027-01-16",
                nullable = true
        )
        String date
) {
}