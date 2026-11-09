package com.uniro.backend.domain.course.controller;

import com.uniro.backend.common.TokenAuthenticator;
import com.uniro.backend.domain.course.dto.CourseSpotsRequest;
import com.uniro.backend.domain.course.service.CourseDraftService;
import com.uniro.backend.domain.course.service.CourseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final CourseDraftService draftService;
    private final TokenAuthenticator authenticator;

    // 코스 조회 (지도)
    @Operation(
            summary = "코스 조회",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/tours/{tourId}/course")
    public ResponseEntity<Map<String, Object>> view(
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @PathVariable Long tourId
    ) {
        var data = courseService.view(authenticator.requireUser(authorization), tourId);

        return response(HttpStatus.OK, "S200", "조회 성공", data);
    }

    // AI 코스 초안 생성
    @Operation(
            summary = "AI 코스 초안 생성",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/tours/{tourId}/course")
    public ResponseEntity<Map<String, Object>> createDraft(
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @PathVariable Long tourId
    ) {
        var data = draftService.create(authenticator.requireUniversity(authorization), tourId);

        return response(HttpStatus.CREATED, "S201", "코스 초안 생성 완료", data);
    }

    // 코스 수정 (지점 전체 교체)
    @Operation(
            summary = "코스 수정",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/tours/{tourId}/course/spots")
    public ResponseEntity<Map<String, Object>> edit(
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @PathVariable Long tourId,
            @RequestBody CourseSpotsRequest request
    ) {
        var data = courseService.edit(
                authenticator.requireUniversity(authorization), tourId, request);

        return response(HttpStatus.OK, "S200", "수정 완료", data);
    }

    // 코스 확정 (쿠폰 발급 포함)
    @Operation(
            summary = "코스 확정",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/tours/{tourId}/course/confirm")
    public ResponseEntity<Map<String, Object>> confirm(
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @PathVariable Long tourId
    ) {
        var data = courseService.confirm(authenticator.requireUniversity(authorization), tourId);

        return response(HttpStatus.OK, "S200", "코스 확정 완료", data);
    }

    private ResponseEntity<Map<String, Object>> response(
            HttpStatus status, String code, String message, Object data
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("data", data);

        return ResponseEntity.status(status).body(body);
    }
}
