package com.uniro.backend.domain.tourwish.service;

import com.uniro.backend.common.ApiException;
import com.uniro.backend.config.JwtProvider;

import com.uniro.backend.domain.verification.entity.StudentVerification;
import com.uniro.backend.domain.verification.entity.VerificationStatus;
import com.uniro.backend.domain.verification.repository.StudentVerificationRepository;

import com.uniro.backend.domain.tourwish.dto.TourWishCreateRequest;
import com.uniro.backend.domain.tourwish.dto.TourWishCreateResponse;
import com.uniro.backend.domain.tourwish.dto.TourWishListResponse;
import com.uniro.backend.domain.tourwish.dto.TourWishUpdateRequest;
import com.uniro.backend.domain.tourwish.dto.TourWishUpdateResponse;
import com.uniro.backend.domain.tourwish.entity.TourWish;
import com.uniro.backend.domain.tourwish.repository.TourWishRepository;

import com.uniro.backend.domain.university.entity.University;
import com.uniro.backend.domain.university.repository.UniversityRepository;

import com.uniro.backend.domain.user.entity.User;
import com.uniro.backend.domain.user.entity.UserRole;
import com.uniro.backend.domain.user.repository.UserRepository;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TourWishService {

    private final TourWishRepository tourWishRepository;
    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;
    private final JwtProvider jwtProvider;
    private final StudentVerificationRepository studentVerificationRepository;

    private static final ZoneId SEOUL =
            ZoneId.of("Asia/Seoul");

    // 기존 탐방 희망 사항 등록 API
    @Transactional
    public TourWishCreateResponse create(
            String authorization,
            TourWishCreateRequest request
    ) {
        User user = authenticateHighSchool(authorization);

        if (request == null) {
            throw invalidRequest(
                    "body",
                    "요청 본문이 필요합니다."
            );
        }

        if (request.campusId() == null) {
            throw invalidRequest(
                    "campus_id",
                    "희망 캠퍼스를 선택해야 합니다."
            );
        }

        if (!universityRepository.existsById(request.campusId())) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "E404",
                    "캠퍼스를 찾을 수 없습니다."
            );
        }

        String major = request.major();
        String wish = request.wish();

        if (major == null
                || major.isBlank()
                || major.length() > 30) {
            throw invalidRequest(
                    "major",
                    "학과는 1~30자로 입력해야 합니다."
            );
        }

        if (wish == null
                || wish.isBlank()
                || wish.length() > 100) {
            throw invalidRequest(
                    "wish",
                    "희망 사항은 1~100자로 입력해야 합니다."
            );
        }

        TourWish tourWish = new TourWish(
                user.getId(),
                request.campusId(),
                major,
                wish,
                request.date()
        );

        TourWish saved =
                tourWishRepository.saveAndFlush(tourWish);

        return new TourWishCreateResponse(
                saved.getId(),
                saved.getUniversityId(),
                saved.getMajor(),
                saved.getContent(),
                saved.getPreferredDate() == null
                        ? null
                        : saved.getPreferredDate().toString(),
                formatDateTime(saved.getCreatedAt())
        );
    }

    // 신규: 내 희망 사항 조회 API
    @Transactional(readOnly = true)
    public TourWishListResponse getMyWishes(
            String authorization,
            int page,
            int size
    ) {
        User user = authenticateHighSchool(authorization);

        if (page < 1 || size < 1) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "E400",
                    "page와 size는 1 이상이어야 합니다."
            );
        }

        PageRequest pageable = PageRequest.of(
                page - 1,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        Page<TourWish> result =
                tourWishRepository.findByUserId(
                        user.getId(),
                        pageable
                );

        List<TourWishListResponse.Item> items =
                result.getContent()
                        .stream()
                        .map(this::toListItem)
                        .toList();

        return new TourWishListResponse(
                items,
                page,
                size,
                result.getTotalElements()
        );
    }

    private TourWishListResponse.Item toListItem(
            TourWish wish
    ) {
        University university = universityRepository
                .findById(wish.getUniversityId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "E404",
                        "대학교를 찾을 수 없습니다."
                ));

        String campusName = university.getName()
                + " "
                + university.getCampusName();

        return new TourWishListResponse.Item(
                wish.getId(),
                campusName.trim(),
                wish.getMajor(),
                wish.getContent(),
                formatDateTime(wish.getCreatedAt())
        );
    }

    private User authenticateHighSchool(
            String authorization
    ) {
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

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "E401",
                        "사용자를 찾을 수 없습니다."
                ));

        if (user.getRole() != UserRole.HIGH_SCHOOL) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "E403",
                    "고등학생만 희망 사항을 조회할 수 있습니다."
            );
        }

        return user;
    }

    private String formatDateTime(
            LocalDateTime dateTime
    ) {
        return dateTime
                .atZone(SEOUL)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private ApiException invalidRequest(
            String field,
            String reason
    ) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "E400",
                "요청 값이 유효하지 않습니다.",
                field,
                reason
        );
    }

    @Transactional
    public TourWishUpdateResponse updateWish(
            String authorization,
            Long wishId,
            TourWishUpdateRequest request
    ) {
        User user = authenticateHighSchool(authorization);

        TourWish wish = tourWishRepository.findById(wishId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "E404",
                        "희망 사항을 찾을 수 없습니다."
                ));

        if (!wish.getUserId().equals(user.getId())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "E403",
                    "본인이 등록한 희망 사항만 수정할 수 있습니다."
            );
        }

        if (request.campusId() != null
                && !universityRepository.existsById(request.campusId())) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "E404",
                    "캠퍼스를 찾을 수 없습니다."
            );
        }

        if (request.major() != null
                && (request.major().isBlank()
                || request.major().length() > 30)) {
            throw invalidRequest(
                    "major",
                    "학과는 1~30자로 입력해야 합니다."
            );
        }

        if (request.wish() != null
                && (request.wish().isBlank()
                || request.wish().length() > 100)) {
            throw invalidRequest(
                    "wish",
                    "희망 사항은 1~100자로 입력해야 합니다."
            );
        }

        wish.update(
                request.campusId(),
                request.major(),
                request.wish(),
                request.date(),
                request.updateDate()
        );

        TourWish saved = tourWishRepository.saveAndFlush(wish);

        University university = universityRepository
                .findById(saved.getUniversityId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "E404",
                        "대학교를 찾을 수 없습니다."
                ));

        String campusName = (
                university.getName() + " " + university.getCampusName()
        ).trim();

        return new TourWishUpdateResponse(
                saved.getId(),
                campusName,
                saved.getMajor(),
                saved.getContent(),
                saved.getPreferredDate() == null
                        ? null
                        : saved.getPreferredDate().toString(),
                formatDateTime(saved.getUpdatedAt())
        );
    }

    @Transactional(readOnly = true)
    public TourWishListResponse getTourWishes(
            String authorization,
            Long campusId,
            String major,
            int page,
            int size
    ) {
        // 1. 로그인 및 대학생 권한 확인
        User user = authenticateUniversity(authorization);

        // 2. 재학 인증 승인 여부 확인
        StudentVerification verification =
                studentVerificationRepository.findById(user.getId())
                        .orElseThrow(() -> new ApiException(
                                HttpStatus.FORBIDDEN,
                                "E403",
                                "재학 인증이 필요합니다."
                        ));

        if (verification.getStatus() != VerificationStatus.APPROVED) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "E403",
                    "재학 인증이 승인된 대학생만 조회할 수 있습니다."
            );
        }

        // 3. 페이지 검증
        if (page < 1 || size < 1) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "E400",
                    "page와 size는 1 이상이어야 합니다."
            );
        }

        // 4. 캠퍼스 결정
        Long targetCampusId = campusId;

        if (targetCampusId == null) {
            targetCampusId = verification.getUniversityId();

            if (targetCampusId == null) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "E400",
                        "인증된 학교 정보가 없습니다."
                );
            }
        }

        // 5. 캠퍼스 존재 여부 확인
        if (!universityRepository.existsById(targetCampusId)) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "E404",
                    "캠퍼스를 찾을 수 없습니다."
            );
        }

        // 6. 검색 조건
        final Long searchCampusId = targetCampusId;
        final String searchMajor = major == null ? null : major.trim();

        Specification<TourWish> specification =
                (root, query, cb) -> {

                    List<Predicate> predicates = new ArrayList<>();

                    predicates.add(
                            cb.equal(
                                    root.get("universityId"),
                                    searchCampusId
                            )
                    );

                    if (searchMajor != null && !searchMajor.isBlank()) {
                        predicates.add(
                                cb.like(
                                        root.get("major"),
                                        "%" + searchMajor + "%"
                                )
                        );
                    }

                    return cb.and(
                            predicates.toArray(new Predicate[0])
                    );
                };

        // 7. 최신순 페이지네이션
        PageRequest pageable = PageRequest.of(
                page - 1,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        Page<TourWish> result =
                tourWishRepository.findAll(specification, pageable);

        // 8. 응답 변환
        List<TourWishListResponse.Item> items =
                result.getContent()
                        .stream()
                        .map(this::toListItem)
                        .toList();

        return new TourWishListResponse(
                items,
                page,
                size,
                result.getTotalElements()
        );
    }

    private User authenticateUniversity(String authorization) {

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

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "E401",
                        "사용자를 찾을 수 없습니다."
                ));

        if (user.getRole() != UserRole.UNIVERSITY) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "E403",
                    "대학생만 희망 사항을 조회할 수 있습니다."
            );
        }

        return user;
    }
}