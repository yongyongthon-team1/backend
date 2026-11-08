package com.uniro.backend.domain.campus.controller;

import com.uniro.backend.common.TokenAuthenticator;
import com.uniro.backend.domain.campus.service.CampusService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/campuses")
@RequiredArgsConstructor
public class CampusController {

    private final CampusService campusService;
    private final TokenAuthenticator authenticator;

    // 캠퍼스 기준 데이터 조회 (로그인 불필요)
    @Operation(summary = "캠퍼스 기준 데이터 조회")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getCampuses() {
        return ok(Map.of("items", campusService.getCampuses()));
    }

    // 캠퍼스 장소 조회 (로그인 필요)
    @Operation(
            summary = "캠퍼스 장소 조회",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{campusId}/places")
    public ResponseEntity<Map<String, Object>> getPlaces(
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @PathVariable Long campusId
    ) {
        authenticator.requireUserId(authorization);

        return ok(Map.of("items", campusService.getPlaces(campusId)));
    }

    private ResponseEntity<Map<String, Object>> ok(Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "S200");
        body.put("message", "조회 성공");
        body.put("data", data);
        return ResponseEntity.ok(body);
    }
}
