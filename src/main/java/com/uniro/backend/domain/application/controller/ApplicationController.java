package com.uniro.backend.domain.application.controller;

import com.uniro.backend.common.TokenAuthenticator;
import com.uniro.backend.domain.application.dto.ApplicationCreateRequest;
import com.uniro.backend.domain.application.dto.ApplicationDecisionRequest;
import com.uniro.backend.domain.application.service.ApplicationService;
import com.uniro.backend.domain.tour.entity.TourApplicationStatus;

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
public class ApplicationController {

    private final ApplicationService applicationService;
    private final TokenAuthenticator authenticator;

    // 탐방 신청
    @Operation(
            summary = "탐방 신청",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/tours/{tourId}/applications")
    public ResponseEntity<Map<String, Object>> apply(
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @PathVariable Long tourId,
            @RequestBody ApplicationCreateRequest request
    ) {
        var data = applicationService.apply(
                authenticator.requireHighSchool(authorization), tourId, request);

        return response(HttpStatus.CREATED, "S201", "신청 완료", data);
    }

    // 신청 승인·거절
    @Operation(
            summary = "신청 승인·거절",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/applications/{applicationId}/decision")
    public ResponseEntity<Map<String, Object>> decide(
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @PathVariable Long applicationId,
            @RequestBody ApplicationDecisionRequest request
    ) {
        var data = applicationService.decide(
                authenticator.requireUniversity(authorization), applicationId, request);

        return response(HttpStatus.OK, "S200", "처리 완료", data);
    }

    // 신청 취소
    @Operation(
            summary = "신청 취소",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/applications/{applicationId}/cancel")
    public ResponseEntity<Map<String, Object>> cancel(
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @PathVariable Long applicationId
    ) {
        var data = applicationService.cancel(
                authenticator.requireHighSchool(authorization), applicationId);

        return response(HttpStatus.OK, "S200", "신청 취소 완료", data);
    }

    // 신청자 목록
    @Operation(
            summary = "신청자 목록 조회",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/tours/{tourId}/applications")
    public ResponseEntity<Map<String, Object>> applicants(
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @PathVariable Long tourId,
            @RequestParam(required = false) TourApplicationStatus status
    ) {
        var data = applicationService.applicants(
                authenticator.requireUniversity(authorization), tourId, status);

        return response(HttpStatus.OK, "S200", "조회 성공", data);
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
