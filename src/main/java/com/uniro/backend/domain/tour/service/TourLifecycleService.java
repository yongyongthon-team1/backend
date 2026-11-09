package com.uniro.backend.domain.tour.service;

import com.uniro.backend.common.ApiException;
import com.uniro.backend.config.JwtProvider;
import com.uniro.backend.domain.coupon.service.CouponService;
import com.uniro.backend.domain.tour.entity.Tour;
import com.uniro.backend.domain.tour.entity.TourApplication;
import com.uniro.backend.domain.tour.entity.TourStatus;
import com.uniro.backend.domain.tour.repository.TourApplicationRepository;
import com.uniro.backend.domain.tour.repository.TourRepository;
import com.uniro.backend.domain.user.entity.User;
import com.uniro.backend.domain.user.entity.UserRole;
import com.uniro.backend.domain.user.repository.UserRepository;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TourLifecycleService {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final TourRepository tourRepository;
    private final TourApplicationRepository applicationRepository;
    private final CouponService couponService;

    private static final ZoneId KOREA_ZONE =
            ZoneId.of("Asia/Seoul");

    // 탐방 취소
    @Transactional
    public Map<String, Object> cancelTour(
            String authorization,
            Long tourId
    ) {
        Long userId = getAuthenticatedUserId(authorization);

        Tour tour = getTourForUpdate(tourId);

        checkGuide(userId, tour);

        // 완료 또는 취소된 탐방은 취소 불가
        if (tour.getStatus() == TourStatus.COMPLETED
                || tour.getStatus() == TourStatus.CANCELED) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "E409_INVALID_TOUR_STATUS",
                    "이미 완료되었거나 취소된 탐방입니다"
            );
        }

        // 탐방 상태 변경
        tour.cancel();

        // 해당 탐방의 신청 내역 전체 취소
        List<TourApplication> applications =
                applicationRepository.findByTourId(tourId);

        for (TourApplication application : applications) {
            application.cancelByTour();
        }

        // 발급된 쿠폰은 만료 처리 (T-05). 이미 사용된 쿠폰은 그대로 둔다.
        couponService.expireIssuedCoupons(
                applications.stream().map(TourApplication::getId).toList());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tour_id", tour.getId());
        data.put("status", tour.getStatus().name());

        return data;
    }

    // 탐방 완료
    @Transactional
    public Map<String, Object> completeTour(
            String authorization,
            Long tourId
    ) {
        Long userId = getAuthenticatedUserId(authorization);

        Tour tour = getTourForUpdate(tourId);

        checkGuide(userId, tour);

        // 취소되었거나 이미 완료된 탐방은 완료 불가
        if (tour.getStatus() == TourStatus.CANCELED
                || tour.getStatus() == TourStatus.COMPLETED) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "E409_INVALID_TOUR_STATUS",
                    "완료 처리할 수 없는 탐방 상태입니다"
            );
        }

        // 탐방 날짜가 지난 후에만 완료 가능
        LocalDate today = LocalDate.now(KOREA_ZONE);
        LocalDate tourDate = tour.getStartsAt().toLocalDate();

        if (!today.isAfter(tourDate)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "E409_INVALID_TOUR_STATUS",
                    "탐방 날짜 이후에만 완료 처리할 수 있어요"
            );
        }

        tour.complete();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tour_id", tour.getId());
        data.put("status", tour.getStatus().name());

        return data;
    }

    // JWT 인증
    private Long getAuthenticatedUserId(String authorization) {

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "E401",
                    "로그인이 필요합니다."
            );
        }

        try {
            String token = authorization.substring(7).trim();

            return jwtProvider.getUserId(token);

        } catch (JwtException | IllegalArgumentException e) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "E401",
                    "유효하지 않거나 만료된 토큰입니다."
            );
        }
    }

    // 탐방 조회 및 동시 변경 방지
    private Tour getTourForUpdate(Long tourId) {
        return tourRepository.findByIdForUpdate(tourId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "E404",
                        "탐방을 찾을 수 없습니다."
                ));
    }

    // 해당 탐방의 가이드인지 확인
    private void checkGuide(Long userId, Tour tour) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "E401",
                        "사용자를 찾을 수 없습니다."
                ));

        if (user.getRole() != UserRole.UNIVERSITY
                || !tour.getGuideId().equals(userId)) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "E403",
                    "해당 탐방의 가이드만 처리할 수 있습니다."
            );
        }
    }
}