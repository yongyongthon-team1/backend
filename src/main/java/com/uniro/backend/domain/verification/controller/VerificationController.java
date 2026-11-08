package com.uniro.backend.domain.verification.controller;

import com.uniro.backend.domain.verification.dto.EnrollmentVerificationResponse;
import com.uniro.backend.domain.verification.dto.BusinessVerificationRequest;
import com.uniro.backend.domain.verification.dto.BusinessVerificationResponse;
import com.uniro.backend.domain.verification.service.EnrollmentVerificationService;
import com.uniro.backend.domain.verification.service.BusinessVerificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/verifications")
@RequiredArgsConstructor
public class VerificationController {

    private final EnrollmentVerificationService enrollmentService;
    private final BusinessVerificationService businessService;

    @Operation(
            summary = "재학 인증 제출",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping(
            value = "/enrollment",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Map<String, Object>> submitEnrollment(
            HttpServletRequest request,

            @RequestParam(
                    value = "campus_id",
                    required = false
            ) Long campusId,

            @RequestParam(
                    value = "school_name",
                    required = false
            ) String schoolName,

            @RequestPart(
                    value = "document",
                    required = false
            ) MultipartFile document
    ) {
        String authorization = request.getHeader("Authorization");

        EnrollmentVerificationResponse data =
                enrollmentService.submit(
                        authorization,
                        campusId,
                        schoolName,
                        document
                );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", "S201");
        response.put("message", "인증 요청 완료");
        response.put("data", data);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(
            summary = "사업자 인증",
            description = "국세청 API로 사업자 정보와 영업 상태를 확인합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping(
            value = "/business",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> verifyBusiness(
            HttpServletRequest request,
            @RequestBody BusinessVerificationRequest body
    ) {
        String authorization = request.getHeader("Authorization");

        BusinessVerificationResponse data =
                businessService.verify(authorization, body);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", "S200");
        response.put("message", "사업자 인증 완료");
        response.put("data", data);

        return ResponseEntity.ok(response);
    }
}