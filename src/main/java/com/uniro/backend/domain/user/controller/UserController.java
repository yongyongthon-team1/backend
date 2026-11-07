package com.uniro.backend.domain.user.controller;

import com.uniro.backend.domain.user.dto.UserMeResponse;
import com.uniro.backend.domain.user.dto.GuideProfileRequest;
import com.uniro.backend.domain.user.dto.GuideProfileResponse;
import com.uniro.backend.domain.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 내 정보 조회
    @Operation(
            summary = "내 정보 조회",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyInfo(
            HttpServletRequest request
    ) {
        String authorization = request.getHeader("Authorization");

        UserMeResponse data = userService.getMyInfo(authorization);

        Map<String, Object> response = new HashMap<>();
        response.put("code", "S200");
        response.put("message", "조회 성공");
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    // 내 정보 수정
    @Operation(
            summary = "내 정보 수정",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/me")
    public ResponseEntity<Map<String, Object>> updateMyInfo(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body
    ) {
        String authorization = request.getHeader("Authorization");

        UserMeResponse data =
                userService.updateMyInfo(authorization, body);

        Map<String, Object> response = new HashMap<>();
        response.put("code", "S200");
        response.put("message", "수정 성공");
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    // 가이드 프로필 등록·수정
    @Operation(
            summary = "가이드 프로필 등록·수정",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/me/guide-profile")
    public ResponseEntity<Map<String, Object>> saveGuideProfile(
            HttpServletRequest request,
            @Valid @RequestBody GuideProfileRequest body
    ) {
        String authorization = request.getHeader("Authorization");

        GuideProfileResponse data =
                userService.saveGuideProfile(authorization, body);

        Map<String, Object> response = new HashMap<>();
        response.put("code", "S200");
        response.put("message", "저장 성공");
        response.put("data", data);

        return ResponseEntity.ok(response);
    }
}