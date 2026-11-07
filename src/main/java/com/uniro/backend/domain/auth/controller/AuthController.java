package com.uniro.backend.domain.auth.controller;

import com.uniro.backend.domain.auth.dto.*;
import com.uniro.backend.domain.auth.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        SignupResponse response = authService.signup(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "code", "S201",
                        "message", "회원가입 성공",
                        "data", response
                ));
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(Map.of(
                "code", "S200",
                "message", "로그인 성공",
                "data", response
        ));
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {

        Map<String, Object> response = new HashMap<>();

        response.put("code", "S200");
        response.put("message", "로그아웃 성공");
        response.put("data", null);

        return ResponseEntity.ok(response);
    }
}