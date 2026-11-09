package com.uniro.backend.domain.store.controller;

import com.uniro.backend.common.TokenAuthenticator;
import com.uniro.backend.domain.store.dto.PartnershipRequest;
import com.uniro.backend.domain.store.dto.StoreRegisterRequest;
import com.uniro.backend.domain.store.dto.StoreUpdateRequest;
import com.uniro.backend.domain.store.service.MyStoreService;
import com.uniro.backend.domain.store.service.StoreReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

// 사장님의 내 가게 API
@RestController
@RequestMapping("/users/me/store")
@RequiredArgsConstructor
public class MyStoreController {

    private final MyStoreService myStoreService;
    private final StoreReportService reportService;
    private final TokenAuthenticator authenticator;

    // 가게 등록 (사장님 계정 연결)
    @Operation(
            summary = "가게 등록 (사장님 계정 연결)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping
    public ResponseEntity<Map<String, Object>> register(
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @RequestBody StoreRegisterRequest request
    ) {
        var data = myStoreService.register(
                authenticator.requireOwner(authorization), request);

        return response(HttpStatus.CREATED, "S201", "가게 등록 성공", data);
    }

    // 내 가게 조회
    @Operation(
            summary = "내 가게 조회",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping
    public ResponseEntity<Map<String, Object>> getMine(
            @RequestHeader(value = "Authorization", required = false)
            String authorization
    ) {
        var data = myStoreService.getMine(
                authenticator.requireOwner(authorization));

        return response(HttpStatus.OK, "S200", "조회 성공", data);
    }

    // 가게 수정
    @Operation(
            summary = "가게 수정",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping
    public ResponseEntity<Map<String, Object>> updateMine(
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @RequestBody StoreUpdateRequest request
    ) {
        var data = myStoreService.updateMine(
                authenticator.requireOwner(authorization), request);

        return response(HttpStatus.OK, "S200", "수정 성공", data);
    }

    // 제휴 할인 설정
    @Operation(
            summary = "제휴 할인 설정",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/partnership")
    public ResponseEntity<Map<String, Object>> savePartnership(
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @RequestBody PartnershipRequest request
    ) {
        var data = myStoreService.savePartnership(
                authenticator.requireOwner(authorization), request);

        return response(HttpStatus.OK, "S200", "저장 성공", data);
    }

    // 방문·리뷰 리포트
    @Operation(
            summary = "방문·리뷰 리포트 조회",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/report")
    public ResponseEntity<Map<String, Object>> report(
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        var data = reportService.report(
                authenticator.requireOwner(authorization), from, to);

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
