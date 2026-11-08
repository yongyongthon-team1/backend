package com.uniro.backend.domain.tourwish.controller;

import com.uniro.backend.domain.tourwish.dto.TourWishCreateRequest;
import com.uniro.backend.domain.tourwish.dto.TourWishCreateResponse;
import com.uniro.backend.domain.tourwish.dto.TourWishListResponse;
import com.uniro.backend.domain.tourwish.service.TourWishService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/tour-wishes")
@RequiredArgsConstructor
public class TourWishController {

    private final TourWishService tourWishService;

    // 기존: 탐방 희망 사항 등록
    @Operation(
            summary = "탐방 희망 사항 등록",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping
    public ResponseEntity<Map<String, Object>> createTourWish(
            HttpServletRequest request,
            @RequestBody TourWishCreateRequest body
    ) {
        TourWishCreateResponse data =
                tourWishService.create(
                        request.getHeader("Authorization"),
                        body
                );

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("code", "S201");
        response.put("message", "등록 성공");
        response.put("data", data);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // 신규: 학교·학과별 고교생 희망 사항 조회
    @Operation(
            summary = "학교·학과별 고교생 희망 사항 조회",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping
    public ResponseEntity<Map<String, Object>> getTourWishes(
            HttpServletRequest request,

            @RequestParam(
                    name = "campus_id",
                    required = false
            )
            Long campusId,

            @RequestParam(required = false)
            String major,

            @RequestParam(defaultValue = "1")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {
        TourWishListResponse data =
                tourWishService.getTourWishes(
                        request.getHeader("Authorization"),
                        campusId,
                        major,
                        page,
                        size
                );

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("code", "S200");
        response.put("message", "조회 성공");
        response.put("data", data);

        return ResponseEntity.ok(response);
    }
}