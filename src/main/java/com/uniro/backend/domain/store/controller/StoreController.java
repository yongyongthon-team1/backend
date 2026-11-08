package com.uniro.backend.domain.store.controller;

import com.uniro.backend.common.TokenAuthenticator;
import com.uniro.backend.domain.store.service.StoreService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;
    private final TokenAuthenticator authenticator;

    // 주변 상점 조회
    @Operation(
            summary = "주변 상점 조회",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/nearby")
    public ResponseEntity<Map<String, Object>> nearby(
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @RequestParam(name = "campus_id", required = false) Long campusId,
            @RequestParam(required = false) String category
    ) {
        authenticator.requireUserId(authorization);

        return ok(Map.of("items", storeService.nearby(campusId, category)));
    }

    // 상점 검색
    @Operation(
            summary = "상점 검색",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        authenticator.requireUserId(authorization);

        return ok(storeService.search(keyword, page, size));
    }

    private ResponseEntity<Map<String, Object>> ok(Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "S200");
        body.put("message", "조회 성공");
        body.put("data", data);

        return ResponseEntity.ok(body);
    }
}
