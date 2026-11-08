package com.uniro.backend.domain.tour.controller;

import com.uniro.backend.domain.tour.entity.TourStatus;
import com.uniro.backend.domain.tour.service.MyTourService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/users/me/tours")
@RequiredArgsConstructor
public class MyTourController {

    private final MyTourService myTourService;

    @Operation(
            summary = "내 탐방 목록 조회",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping
    public ResponseEntity<Map<String, Object>> getMyTours(
            HttpServletRequest request,
            @RequestParam(required = false) TourStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Map<String, Object> data = myTourService.getMyTours(
                request.getHeader("Authorization"),
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
}