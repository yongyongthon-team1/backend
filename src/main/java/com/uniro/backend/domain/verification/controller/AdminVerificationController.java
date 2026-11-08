package com.uniro.backend.domain.verification.controller;

import com.uniro.backend.domain.verification.dto.AdminVerificationResponse;
import com.uniro.backend.domain.verification.dto.AdminVerificationDetailResponse;
import com.uniro.backend.domain.verification.dto.AdminVerificationReviewRequest;
import com.uniro.backend.domain.verification.dto.AdminVerificationReviewResponse;
import com.uniro.backend.domain.verification.entity.VerificationStatus;
import com.uniro.backend.domain.verification.service.AdminVerificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/verifications")
@RequiredArgsConstructor
public class AdminVerificationController {

    private final AdminVerificationService adminVerificationService;

    @Operation(
            summary = "인증 심사 목록 조회",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping
    public ResponseEntity<Map<String, Object>> getVerifications(
            HttpServletRequest request,
            @RequestParam(defaultValue = "PENDING")
            VerificationStatus status,
            @RequestParam(defaultValue = "1")
            int page,
            @RequestParam(defaultValue = "20")
            int size
    ) {
        AdminVerificationResponse data =
                adminVerificationService.getList(
                        request.getHeader("Authorization"),
                        status,
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
            summary = "인증 심사 상세 조회",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{verificationId}")
    public ResponseEntity<Map<String, Object>> getVerificationDetail(
            HttpServletRequest request,
            @PathVariable Long verificationId
    ) {
        AdminVerificationDetailResponse data =
                adminVerificationService.getDetail(
                        request.getHeader("Authorization"),
                        verificationId
                );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", "S200");
        response.put("message", "조회 성공");
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "인증 심사 승인·반려",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/{verificationId}")
    public ResponseEntity<Map<String, Object>> reviewVerification(
            HttpServletRequest request,
            @PathVariable Long verificationId,
            @RequestBody AdminVerificationReviewRequest body
    ) {
        AdminVerificationReviewResponse data =
                adminVerificationService.review(
                        request.getHeader("Authorization"),
                        verificationId,
                        body
                );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", "S200");
        response.put("message", "심사 완료");
        response.put("data", data);

        return ResponseEntity.ok(response);
    }
}