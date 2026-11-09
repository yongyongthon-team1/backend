package com.uniro.backend.domain.tour.controller;

import com.uniro.backend.domain.tour.service.TourLifecycleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/tours")
@RequiredArgsConstructor
public class TourLifecycleController {

    private final TourLifecycleService tourLifecycleService;

    @Operation(
            summary = "탐방 취소",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{tourId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelTour(
            HttpServletRequest request,
            @PathVariable Long tourId
    ) {
        Map<String, Object> data =
                tourLifecycleService.cancelTour(
                        request.getHeader("Authorization"),
                        tourId
                );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", "S200");
        response.put("message", "탐방 취소 완료");
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "탐방 완료 처리",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{tourId}/complete")
    public ResponseEntity<Map<String, Object>> completeTour(
            HttpServletRequest request,
            @PathVariable Long tourId
    ) {
        Map<String, Object> data =
                tourLifecycleService.completeTour(
                        request.getHeader("Authorization"),
                        tourId
                );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", "S200");
        response.put("message", "탐방 완료 처리");
        response.put("data", data);

        return ResponseEntity.ok(response);
    }
}