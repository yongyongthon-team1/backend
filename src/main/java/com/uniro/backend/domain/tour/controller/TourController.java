package com.uniro.backend.domain.tour.controller;

import com.uniro.backend.domain.tour.dto.TourCreateRequest;
import com.uniro.backend.domain.tour.dto.TourCreateResponse;
import com.uniro.backend.domain.tour.dto.TourListResponse;
import com.uniro.backend.domain.tour.dto.TourDetailResponse;
import com.uniro.backend.domain.tour.entity.TourStatus;
import com.uniro.backend.domain.tour.service.TourService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/tours")
@RequiredArgsConstructor
public class TourController {

    private final TourService tourService;

    @Operation(
            summary = "탐방 목록 조회",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping
    public ResponseEntity<Map<String, Object>> getTours(
            HttpServletRequest request,

            @RequestParam(name = "campus_id", required = false)
            Long campusId,

            @RequestParam(required = false)
            String major,

            @RequestParam(name = "date_from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateFrom,

            @RequestParam(name = "date_to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateTo,

            @RequestParam(defaultValue = "RECRUITING")
            TourStatus status,

            @RequestParam(defaultValue = "1")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {
        TourListResponse data = tourService.getTours(
                request.getHeader("Authorization"),
                campusId,
                major,
                dateFrom,
                dateTo,
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
            summary = "탐방 상세 조회",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{tourId}")
    public ResponseEntity<Map<String, Object>> getTourDetail(
            HttpServletRequest request,
            @PathVariable Long tourId
    ) {
        TourDetailResponse data = tourService.getTourDetail(
                request.getHeader("Authorization"),
                tourId
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", "S200");
        response.put("message", "조회 성공");
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "탐방 개설",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping
    public ResponseEntity<Map<String, Object>> createTour(
            HttpServletRequest request,
            @RequestBody TourCreateRequest body
    ) {
        TourCreateResponse data = tourService.createTour(
                request.getHeader("Authorization"),
                body
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", "S201");
        response.put("message", "탐방 개설 성공");
        response.put("data", data);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}