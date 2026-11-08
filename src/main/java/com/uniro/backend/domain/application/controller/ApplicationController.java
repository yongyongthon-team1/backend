package com.uniro.backend.domain.application.controller;

import com.uniro.backend.common.TokenAuthenticator;
import com.uniro.backend.domain.application.dto.ApplicationCreateRequest;
import com.uniro.backend.domain.application.service.ApplicationService;

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
