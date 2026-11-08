package com.uniro.backend.domain.tour.service;

import com.uniro.backend.common.ApiException;
import com.uniro.backend.config.JwtProvider;

import com.uniro.backend.domain.tour.dto.TourListResponse;
import com.uniro.backend.domain.tour.dto.TourDetailResponse;
import com.uniro.backend.domain.tour.dto.TourCreateRequest;
import com.uniro.backend.domain.tour.dto.TourCreateResponse;
import com.uniro.backend.domain.tour.entity.TourApplication;
import com.uniro.backend.domain.tour.entity.Tour;
import com.uniro.backend.domain.tour.entity.TourApplicationStatus;
import com.uniro.backend.domain.tour.entity.TourStatus;
import com.uniro.backend.domain.tour.repository.TourRepository;
import com.uniro.backend.domain.tour.repository.TourApplicationRepository;

import com.uniro.backend.domain.university.entity.University;
import com.uniro.backend.domain.university.repository.UniversityRepository;
import com.uniro.backend.domain.university.repository.UniversityVacationRepository;

import com.uniro.backend.domain.user.entity.User;
import com.uniro.backend.domain.user.entity.GuideProfile;
import com.uniro.backend.domain.user.entity.UserRole;
import com.uniro.backend.domain.user.repository.UserRepository;
import com.uniro.backend.domain.user.repository.GuideProfileRepository;

import com.uniro.backend.domain.verification.entity.StudentVerification;
import com.uniro.backend.domain.verification.entity.VerificationStatus;
import com.uniro.backend.domain.verification.repository.StudentVerificationRepository;

import io.jsonwebtoken.JwtException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TourService {

    private final TourRepository tourRepository;
    private final TourApplicationRepository applicationRepository;
    private final UniversityRepository universityRepository;
    private final UserRepository userRepository;
    private final GuideProfileRepository guideProfileRepository;
    private final StudentVerificationRepository verificationRepository;
    private final JwtProvider jwtProvider;
    private final UniversityVacationRepository universityVacationRepository;

    @Transactional(readOnly = true)
    public TourListResponse getTours(
            String authorization,
            Long campusId,
            String major,
            LocalDate dateFrom,
            LocalDate dateTo,
            TourStatus status,
            int page,
            int size
    ) {
        authenticate(authorization);

        if (page < 1 || size < 1) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "E400",
                    "page와 size는 1 이상이어야 합니다."
            );
        }

        if (dateFrom != null
                && dateTo != null
                && dateFrom.isAfter(dateTo)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "E400",
                    "시작 날짜는 종료 날짜보다 늦을 수 없습니다."
            );
        }

        String keyword = major == null ? null : major.trim();

        Specification<Tour> specification = (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(
                    cb.equal(root.get("status"), status)
            );

            if (campusId != null) {
                predicates.add(
                        cb.equal(root.get("universityId"), campusId)
                );
            }

            if (dateFrom != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.get("startsAt"),
                                dateFrom.atStartOfDay()
                        )
                );
            }

            if (dateTo != null) {
                predicates.add(
                        cb.lessThan(
                                root.get("startsAt"),
                                dateTo.plusDays(1).atStartOfDay()
                        )
                );
            }

            if (keyword != null && !keyword.isBlank()) {

                var subquery = query.subquery(Long.class);
                var profile = subquery.from(GuideProfile.class);

                subquery.select(profile.get("id"))
                        .where(
                                cb.equal(
                                        profile.get("id"),
                                        root.get("guideId")
                                ),
                                cb.like(
                                        profile.get("major"),
                                        "%" + keyword + "%"
                                )
                        );

                predicates.add(cb.exists(subquery));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageable = PageRequest.of(
                page - 1,
                size,
                Sort.by(
                        Sort.Order.asc("startsAt"),
                        Sort.Order.asc("id")
                )
        );

        Page<Tour> result =
                tourRepository.findAll(specification, pageable);

        List<TourListResponse.Item> items =
                result.getContent()
                        .stream()
                        .map(this::toItem)
                        .toList();

        return new TourListResponse(
                items,
                page,
                size,
                result.getTotalElements()
        );
    }

    private TourListResponse.Item toItem(Tour tour) {

        University university = universityRepository
                .findById(tour.getUniversityId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "E404",
                        "대학교를 찾을 수 없습니다."
                ));

        User guide = userRepository
                .findById(tour.getGuideId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "E404",
                        "가이드를 찾을 수 없습니다."
                ));

        GuideProfile profile = guideProfileRepository
                .findById(tour.getGuideId())
                .orElse(null);

        boolean verified = verificationRepository
                .findById(tour.getGuideId())
                .map(v -> v.getStatus() == VerificationStatus.APPROVED)
                .orElse(false);

        long activeCount = applicationRepository
                .countByTourIdAndStatusIn(
                        tour.getId(),
                        List.of(
                                TourApplicationStatus.PENDING,
                                TourApplicationStatus.APPROVED
                        )
                );

        int remaining = (int) Math.max(
                0L,
                (long) tour.getCapacity() - activeCount
        );

        LocalDateTime startsAt = tour.getStartsAt();

        String campusName = university.getName()
                + " "
                + university.getCampusName();

        return new TourListResponse.Item(
                tour.getId(),
                campusName.trim(),
                profile == null ? null : profile.getMajor(),
                startsAt.toLocalDate().toString(),
                startsAt.toLocalTime()
                        .format(DateTimeFormatter.ofPattern("HH:mm")),
                tour.getCapacity(),
                remaining,
                tour.getStatus().name(),
                new TourListResponse.Guide(
                        guide.getName(),
                        verified
                )
        );
    }

    private void authenticate(String authorization) {

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "E401",
                    "인증이 필요합니다."
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

        if (!userRepository.existsById(userId)) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "E401",
                    "사용자를 찾을 수 없습니다."
            );
        }
    }

    @Transactional(readOnly = true)
    public TourDetailResponse getTourDetail(
            String authorization,
            Long tourId
    ) {
        // 1. 로그인 사용자 확인
        Long userId = authenticateAndGetUserId(authorization);

        // 2. 탐방 조회
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "E404",
                        "탐방을 찾을 수 없습니다."
                ));

        // 3. 대학교 조회
        University university = universityRepository
                .findById(tour.getUniversityId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "E404",
                        "대학교를 찾을 수 없습니다."
                ));

        String campusName = (
                university.getName() + " " + university.getCampusName()
        ).trim();

        // 4. 가이드 조회
        User guide = userRepository
                .findById(tour.getGuideId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "E404",
                        "가이드를 찾을 수 없습니다."
                ));

        GuideProfile profile = guideProfileRepository
                .findById(tour.getGuideId())
                .orElse(null);

        String guideMajor = profile == null
                ? null
                : profile.getMajor();

        String guideIntro = profile == null
                ? null
                : profile.getIntroduction();

        // 5. 가이드 재학 인증 여부
        boolean verified = verificationRepository
                .findById(tour.getGuideId())
                .map(v -> v.getStatus() == VerificationStatus.APPROVED)
                .orElse(false);

        // 6. 남은 자리 계산
        long activeCount = applicationRepository
                .countByTourIdAndStatusIn(
                        tour.getId(),
                        List.of(
                                TourApplicationStatus.PENDING,
                                TourApplicationStatus.APPROVED
                        )
                );

        int remaining = (int) Math.max(
                0L,
                (long) tour.getCapacity() - activeCount
        );

        // 7. 내 신청 내역 조회
        Optional<TourApplication> myApplication =
                applicationRepository
                        .findTopByTourIdAndUserIdOrderByCreatedAtDescIdDesc(
                                tour.getId(),
                                userId
                        );

        TourDetailResponse.MyApplication applicationResponse =
                myApplication
                        .map(application ->
                                new TourDetailResponse.MyApplication(
                                        application.getId(),
                                        application.getStatus().name()
                                )
                        )
                        .orElse(null);

        // 8. 오픈채팅 링크 접근 권한 확인
        boolean isGuide = tour.getGuideId().equals(userId);

        boolean isApprovedApplicant = myApplication
                .map(application ->
                        application.getStatus()
                                == TourApplicationStatus.APPROVED
                )
                .orElse(false);

        String openChatUrl =
                (isGuide || isApprovedApplicant)
                        ? tour.getOpenChatUrl()
                        : null;

        // 9. 날짜 및 시간
        LocalDateTime startsAt = tour.getStartsAt();
        LocalDateTime endsAt = tour.getEndsAt();

        String date = startsAt.toLocalDate().toString();

        String startTime = startsAt.toLocalTime()
                .format(DateTimeFormatter.ofPattern("HH:mm"));

        String endTime = endsAt == null
                ? null
                : endsAt.toLocalTime()
                .format(DateTimeFormatter.ofPattern("HH:mm"));

        // 10. 응답 생성
        return new TourDetailResponse(
                tour.getId(),
                campusName,
                guideMajor,
                date,
                startTime,
                endTime,
                tour.getCapacity(),
                remaining,
                tour.getStatus().name(),
                tour.getIntroduction(),
                openChatUrl,
                new TourDetailResponse.Guide(
                        guide.getName(),
                        guideMajor,
                        guideIntro,
                        verified
                ),
                applicationResponse
        );
    }

    private Long authenticateAndGetUserId(String authorization) {

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "E401",
                    "인증이 필요합니다."
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

        if (!userRepository.existsById(userId)) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "E401",
                    "사용자를 찾을 수 없습니다."
            );
        }

        return userId;
    }

    @Transactional
    public TourCreateResponse createTour(
            String authorization,
            TourCreateRequest request
    ) {
        // 1. JWT 인증
        Long userId = authenticateAndGetUserId(authorization);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "E401",
                        "사용자를 찾을 수 없습니다."
                ));

        // 2. 대학생 역할 확인
        if (user.getRole() != UserRole.UNIVERSITY) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "E403",
                    "대학생만 탐방을 개설할 수 있습니다."
            );
        }

        // 3. 재학 인증 확인
        StudentVerification verification =
                verificationRepository.findById(userId)
                        .orElseThrow(() -> new ApiException(
                                HttpStatus.FORBIDDEN,
                                "E403",
                                "재학 인증이 필요합니다."
                        ));

        if (verification.getStatus() != VerificationStatus.APPROVED) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "E403",
                    "재학 인증이 승인된 대학생만 탐방을 개설할 수 있습니다."
            );
        }

        Long universityId = verification.getUniversityId();

        if (universityId == null
                || !universityRepository.existsById(universityId)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "E400",
                    "인증된 학교 정보가 유효하지 않습니다."
            );
        }

        // 4. 가이드 프로필 확인
        GuideProfile guideProfile =
                guideProfileRepository.findById(userId)
                        .orElseThrow(() -> new ApiException(
                                HttpStatus.CONFLICT,
                                "E409_GUIDE_PROFILE_REQUIRED",
                                "가이드 프로필을 먼저 등록해 주세요"
                        ));

        // 학과는 가이드 프로필에서 가져옴
        if (guideProfile.getMajor() == null
                || guideProfile.getMajor().isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "E400",
                    "가이드 프로필에 학과를 등록해 주세요."
            );
        }

        // 5. 필수 입력 검증
        if (request == null) {
            throw invalidTourRequest("body", "요청 본문이 필요합니다.");
        }

        LocalDate date = request.date();
        LocalTime startTime = request.startTime();
        LocalTime endTime = request.endTime();

        if (date == null) {
            throw invalidTourRequest("date", "날짜를 입력해 주세요.");
        }

        if (startTime == null) {
            throw invalidTourRequest(
                    "start_time",
                    "시작 시간을 입력해 주세요."
            );
        }

        if (endTime == null || !endTime.isAfter(startTime)) {
            throw invalidTourRequest(
                    "end_time",
                    "종료 시간은 시작 시간 이후여야 합니다."
            );
        }

        if (request.capacity() == null
                || request.capacity() < 1
                || request.capacity() > 10) {
            throw invalidTourRequest(
                    "capacity",
                    "정원은 1~10명이어야 합니다."
            );
        }

        if (request.intro() == null
                || request.intro().isBlank()
                || request.intro().length() > 300) {
            throw invalidTourRequest(
                    "intro",
                    "탐방 소개는 1~300자로 입력해야 합니다."
            );
        }

        if (request.openChatUrl() == null
                || request.openChatUrl().isBlank()
                || request.openChatUrl().length() > 500) {
            throw invalidTourRequest(
                    "open_chat_url",
                    "오픈채팅 링크는 필수이며 500자 이내여야 합니다."
            );
        }

        // 6. 방학 기간 확인
        boolean withinVacation =
                universityVacationRepository
                        .existsByUniversityIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                                universityId,
                                date,
                                date
                        );

        if (!withinVacation) {
            throw invalidTourRequest(
                    "date",
                    "방학 기간 안의 날짜만 선택할 수 있어요"
            );
        }

        // 7. 날짜 + 시간 조합
        LocalDateTime startsAt = LocalDateTime.of(date, startTime);
        LocalDateTime endsAt = LocalDateTime.of(date, endTime);

        // 8. 탐방 생성
        Tour tour = new Tour(
                userId,
                universityId,
                startsAt,
                endsAt,
                request.capacity(),
                request.intro(),
                request.openChatUrl()
        );

        Tour saved = tourRepository.save(tour);

        return new TourCreateResponse(
                saved.getId(),
                saved.getStatus().name()
        );
    }

    private ApiException invalidTourRequest(
            String field,
            String reason
    ) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "E400",
                "요청 값이 유효하지 않습니다",
                field,
                reason
        );
    }
}