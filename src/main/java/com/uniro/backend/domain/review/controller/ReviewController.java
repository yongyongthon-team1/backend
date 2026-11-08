package com.uniro.backend.domain.review.controller;

import com.uniro.backend.common.TokenAuthenticator;
import com.uniro.backend.domain.review.dto.ReviewCreateRequest;
import com.uniro.backend.domain.review.service.ReviewService;

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
public class ReviewController {

    private final ReviewService reviewService;
    private final TokenAuthenticator authenticator;

    // 리뷰 작성
    @Operation(
            summary = "리뷰 작성",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/stores/{storeId}/reviews")
    public ResponseEntity<Map<String, Object>> create(
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @PathVariable Long storeId,
            @RequestBody ReviewCreateRequest request
    ) {
        var data = reviewService.create(
                authenticator.requireHighSchool(authorization), storeId, request);

        return response(HttpStatus.CREATED, "S201", "리뷰 작성 성공", data);
    }

    // 상점 리뷰·요약 조회
    @Operation(
            summary = "상점 리뷰·요약 조회",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/stores/{storeId}/reviews")
    public ResponseEntity<Map<String, Object>> list(
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @PathVariable Long storeId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        authenticator.requireUserId(authorization);

        return response(HttpStatus.OK, "S200", "조회 성공", reviewService.list(storeId, page, size));
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
