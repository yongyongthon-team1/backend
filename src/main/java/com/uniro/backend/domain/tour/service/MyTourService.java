package com.uniro.backend.domain.tour.service;

import com.uniro.backend.common.ApiException;
import com.uniro.backend.config.JwtProvider;
import com.uniro.backend.domain.tour.entity.*;
import com.uniro.backend.domain.tour.repository.*;
import com.uniro.backend.domain.university.repository.UniversityRepository;
import com.uniro.backend.domain.user.entity.User;
import com.uniro.backend.domain.user.entity.UserRole;
import com.uniro.backend.domain.user.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyTourService {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final TourRepository tourRepository;
    private final TourApplicationRepository applicationRepository;
    private final UniversityRepository universityRepository;

    public Map<String, Object> getMyTours(
            String authorization,
            TourStatus status,
            int page,
            int size
    ) {
        if (authorization == null
                || !authorization.startsWith("Bearer ")) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "E401",
                    "로그인이 필요합니다."
            );
        }

        Long userId;

        try {
            userId = jwtProvider.getUserId(
                    authorization.substring(7).trim()
            );
        } catch (JwtException | IllegalArgumentException e) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "E401",
                    "유효하지 않거나 만료된 토큰입니다."
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "E401",
                        "사용자를 찾을 수 없습니다."
                ));

        if (page < 1 || size < 1) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "E400",
                    "page와 size는 1 이상이어야 합니다."
            );
        }

        if (user.getRole() == UserRole.UNIVERSITY) {
            return getGuideTours(userId, status, page, size);
        }

        if (user.getRole() == UserRole.HIGH_SCHOOL) {
            return getStudentTours(userId, status, page, size);
        }

        throw new ApiException(
                HttpStatus.FORBIDDEN,
                "E403",
                "고교생 또는 대학생만 조회할 수 있습니다."
        );
    }

    private Map<String, Object> getGuideTours(
            Long userId,
            TourStatus status,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
                page - 1,
                size,
                Sort.by(Sort.Direction.ASC, "startsAt")
        );

        Page<Tour> tours = status == null
                ? tourRepository.findByGuideId(userId, pageable)
                : tourRepository.findByGuideIdAndStatus(
                userId, status, pageable
        );

        List<Map<String, Object>> items = tours.getContent()
                .stream()
                .map(tour -> {
                    Map<String, Object> item = new LinkedHashMap<>();

                    item.put("tour_id", tour.getId());
                    item.put("date", tour.getStartsAt().toLocalDate());
                    item.put("tour_status", tour.getStatus().name());
                    item.put("capacity", tour.getCapacity());

                    item.put(
                            "approved_count",
                            applicationRepository.countByTourIdAndStatus(
                                    tour.getId(),
                                    TourApplicationStatus.APPROVED
                            )
                    );

                    item.put(
                            "pending_count",
                            applicationRepository.countByTourIdAndStatus(
                                    tour.getId(),
                                    TourApplicationStatus.PENDING
                            )
                    );

                    // 코스 담당 팀원의 API 연동 후 변경
                    item.put("course_confirmed", null);

                    return item;
                })
                .toList();

        return pageResponse(items, page, size, tours.getTotalElements());
    }

    private Map<String, Object> getStudentTours(
            Long userId,
            TourStatus status,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
                page - 1,
                size,
                Sort.by(
                        Sort.Direction.ASC, "createdAt"
                )
        );

        Page<TourApplication> applications =
                applicationRepository.findByUserId(userId, pageable);

        List<Map<String, Object>> items = applications.getContent()
                .stream()
                .map(application -> {
                    Tour tour = tourRepository.findById(
                            application.getTourId()
                    ).orElse(null);

                    if (tour == null) {
                        return null;
                    }

                    if (status != null && tour.getStatus() != status) {
                        return null;
                    }

                    String campusName = universityRepository
                            .findById(tour.getUniversityId())
                            .map(university ->
                                    university.getName() + " " +
                                            university.getCampusName()
                            )
                            .orElse(null);

                    Map<String, Object> item = new LinkedHashMap<>();

                    item.put("tour_id", tour.getId());
                    item.put("campus_name", campusName);
                    item.put("date", tour.getStartsAt().toLocalDate());
                    item.put("tour_status", tour.getStatus().name());
                    item.put("application_id", application.getId());
                    item.put(
                            "application_status",
                            application.getStatus().name()
                    );
                    item.put("course_confirmed", null);

                    return item;
                })
                .filter(item -> item != null)
                .toList();

        return pageResponse(
                items,
                page,
                size,
                applications.getTotalElements()
        );
    }

    private Map<String, Object> pageResponse(
            List<Map<String, Object>> items,
            int page,
            int size,
            long total
    ) {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("items", items);
        result.put("page", page);
        result.put("size", size);
        result.put("total", total);

        return result;
    }
}