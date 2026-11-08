package com.uniro.backend.domain.tourwish.controller;

import com.uniro.backend.common.ApiException;
import com.uniro.backend.domain.tourwish.dto.TourWishListResponse;
import com.uniro.backend.domain.tourwish.dto.TourWishUpdateRequest;
import com.uniro.backend.domain.tourwish.dto.TourWishUpdateResponse;
import com.uniro.backend.domain.tourwish.dto.TourWishUpdateSwaggerRequest;
import com.uniro.backend.domain.tourwish.service.TourWishService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/users/me/tour-wishes")
@RequiredArgsConstructor
public class MyTourWishController {

    private final TourWishService tourWishService;

    @Operation(
            summary = "내 희망 사항 조회",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping
    public ResponseEntity<Map<String, Object>> getMyTourWishes(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        TourWishListResponse data =
                tourWishService.getMyWishes(
                        request.getHeader("Authorization"),
                        page,
                        size
                );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", "S200");
        response.put("message", "조회 성공");
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "탐방 희망 사항 수정",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = TourWishUpdateSwaggerRequest.class
                            )
                    )
            )
    )
    @PatchMapping("/{wishId}")
    public ResponseEntity<Map<String, Object>> updateTourWish(
            HttpServletRequest request,
            @PathVariable Long wishId,
            @org.springframework.web.bind.annotation.RequestBody JsonNode body
    ) {
        if (body == null || !body.isObject()) {
            throw invalidRequest(
                    "body",
                    "JSON 객체를 입력해야 합니다."
            );
        }

        Long campusId = null;
        String major = null;
        String wish = null;
        LocalDate date = null;

        if (body.has("campus_id")) {
            JsonNode value = body.get("campus_id");

            if (!value.isIntegralNumber() || !value.canConvertToLong()) {
                throw invalidRequest(
                        "campus_id",
                        "캠퍼스 ID는 정수여야 합니다."
                );
            }

            campusId = value.longValue();

            if (campusId <= 0) {
                throw invalidRequest(
                        "campus_id",
                        "캠퍼스 ID는 1 이상이어야 합니다."
                );
            }
        }

        if (body.has("major")) {
            JsonNode value = body.get("major");

            if (!value.isTextual()) {
                throw invalidRequest(
                        "major",
                        "학과는 문자열이어야 합니다."
                );
            }

            major = value.asText();
        }

        if (body.has("wish")) {
            JsonNode value = body.get("wish");

            if (!value.isTextual()) {
                throw invalidRequest(
                        "wish",
                        "희망 사항은 문자열이어야 합니다."
                );
            }

            wish = value.asText();
        }

        boolean updateDate = body.has("date");

        if (updateDate) {
            JsonNode value = body.get("date");

            if (!value.isNull()) {
                if (!value.isTextual()) {
                    throw invalidRequest(
                            "date",
                            "날짜는 yyyy-MM-dd 형식이어야 합니다."
                    );
                }

                try {
                    date = LocalDate.parse(value.asText());
                } catch (DateTimeParseException e) {
                    throw invalidRequest(
                            "date",
                            "날짜는 yyyy-MM-dd 형식이어야 합니다."
                    );
                }
            }
        }

        TourWishUpdateRequest updateRequest =
                new TourWishUpdateRequest(
                        campusId,
                        major,
                        wish,
                        date,
                        updateDate
                );

        TourWishUpdateResponse data =
                tourWishService.updateWish(
                        request.getHeader("Authorization"),
                        wishId,
                        updateRequest
                );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", "S200");
        response.put("message", "수정 성공");
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    private ApiException invalidRequest(
            String field,
            String reason
    ) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "E400",
                "요청 값이 유효하지 않습니다.",
                field,
                reason
        );
    }
}