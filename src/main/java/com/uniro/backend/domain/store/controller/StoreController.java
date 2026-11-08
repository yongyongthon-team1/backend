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

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "S200");
        body.put("message", "조회 성공");
        body.put("data", storeService.search(keyword, page, size));

        return ResponseEntity.ok(body);
    }
}
