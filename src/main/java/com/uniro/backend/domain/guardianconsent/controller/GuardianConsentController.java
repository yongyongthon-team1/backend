
package com.uniro.backend.domain.guardianconsent.controller;

import com.uniro.backend.common.ApiException;
import com.uniro.backend.config.JwtProvider;
import com.uniro.backend.domain.guardianconsent.dto.GuardianConsentGuideResponse;
import com.uniro.backend.domain.guardianconsent.dto.GuardianConsentRequest;
import com.uniro.backend.domain.guardianconsent.dto.GuardianConsentResponse;
import com.uniro.backend.domain.guardianconsent.dto.GuardianConsentDecisionRequest;
import com.uniro.backend.domain.guardianconsent.dto.GuardianConsentDecisionResponse;
import com.uniro.backend.domain.guardianconsent.service.GuardianConsentService;

import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/guardian-consents")
@RequiredArgsConstructor
public class GuardianConsentController {

    private final GuardianConsentService consentService;
    private final JwtProvider jwtProvider;

    @Operation(
            summary = "보호자 동의 요청",
            description = "고등학생이 보호자에게 동의 이메일을 발송합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping
    public ResponseEntity<Map<String, Object>> requestConsent(
            HttpServletRequest httpRequest,
            @RequestBody GuardianConsentRequest request
    ) {
        String authorization =
                httpRequest.getHeader("Authorization");

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "E401",
                    "인증 토큰이 필요합니다"
            );
        }

        String token = authorization.substring(7);

        Long userId;
        try {
            userId = jwtProvider.getUserId(token);
        } catch (JwtException | IllegalArgumentException e) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "E401",
                    "유효하지 않거나 만료된 토큰입니다"
            );
        }

        GuardianConsentResponse response =
                consentService.requestConsent(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of(
                        "code", "S201",
                        "message", "동의 요청 완료",
                        "data", response
                )
        );
    }

    @Operation(
            summary = "보호자 동의 안내 조회",
            description = "보호자가 동의 토큰으로 학생 정보와 탐방 안내를 조회합니다."
    )
    @GetMapping("/{consentToken}")
    public ResponseEntity<Map<String, Object>> getConsentGuide(
            @PathVariable String consentToken
    ) {
        GuardianConsentGuideResponse response =
                consentService.getConsentGuide(consentToken);

        return ResponseEntity.ok(
                Map.of(
                        "code", "S200",
                        "message", "조회 성공",
                        "data", response
                )
        );
    }

    @Operation(
            summary = "보호자 동의·거부",
            description = "보호자가 로그인 없이 동의 또는 거부를 처리합니다."
    )
    @PatchMapping("/{consentToken}")
    public ResponseEntity<Map<String, Object>> respondConsent(
            @PathVariable String consentToken,
            @RequestBody GuardianConsentDecisionRequest request
    ) {
        GuardianConsentDecisionResponse response =
                consentService.respondConsent(consentToken, request);

        return ResponseEntity.ok(
                Map.of(
                        "code", "S200",
                        "message", "처리 완료",
                        "data", response
                )
        );
    }
}
