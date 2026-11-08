package com.uniro.backend.domain.course.controller;

import com.uniro.backend.common.TokenAuthenticator;
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
