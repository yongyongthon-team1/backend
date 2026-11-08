package com.uniro.backend.domain.verification.service;

import com.uniro.backend.common.ApiException;
import com.uniro.backend.config.JwtProvider;
import com.uniro.backend.domain.university.entity.University;
import com.uniro.backend.domain.university.repository.UniversityRepository;
import com.uniro.backend.domain.user.entity.User;
import com.uniro.backend.domain.user.entity.UserRole;
import com.uniro.backend.domain.user.repository.UserRepository;
import com.uniro.backend.domain.verification.dto.AdminVerificationResponse;
import com.uniro.backend.domain.verification.dto.AdminVerificationDetailResponse;
import com.uniro.backend.domain.verification.dto.AdminVerificationReviewRequest;
import com.uniro.backend.domain.verification.dto.AdminVerificationReviewResponse;
import com.uniro.backend.domain.verification.entity.StudentVerification;
import com.uniro.backend.domain.verification.entity.VerificationStatus;
import com.uniro.backend.domain.verification.repository.StudentVerificationRepository;
import com.uniro.backend.storage.S3DocumentStorage;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminVerificationService {

    private static final Logger log =
            LoggerFactory.getLogger(AdminVerificationService.class);

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final StudentVerificationRepository verificationRepository;
    private final UniversityRepository universityRepository;
    private final S3DocumentStorage storage;

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Transactional(readOnly = true)
    public AdminVerificationResponse getList(
            String authorization,
            VerificationStatus status,
            int page,
            int size
    ) {
        authenticateAdmin(authorization);

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
                        Sort.Order.asc("submittedAt"),
                        Sort.Order.asc("id")
                )
        );

        Page<StudentVerification> result =
                verificationRepository.findByStatus(status, pageable);

        List<AdminVerificationResponse.Item> items =
                result.getContent()
                        .stream()
                        .map(verification -> {
                            User user = findUser(verification.getId());

                            return new AdminVerificationResponse.Item(
                                    verification.getId(),
                                    user.getName(),
                                    user.getRole().name(),
                                    getSchoolName(verification, user.getRole()),
                                    formatDateTime(verification.getSubmittedAt())
                            );
                        })
                        .toList();

        return new AdminVerificationResponse(
                items,
                page,
                size,
                result.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public AdminVerificationDetailResponse getDetail(
            String authorization,
            Long verificationId
    ) {
        authenticateAdmin(authorization);

        StudentVerification verification =
                verificationRepository.findById(verificationId)
                        .orElseThrow(() -> new ApiException(
                                HttpStatus.NOT_FOUND,
                                "E404",
                                "인증 요청을 찾을 수 없습니다."
                        ));

        User user = findUser(verification.getId());

        String documentUrl = null;

        if (verification.getDocumentUrl() != null) {
            try {
                documentUrl = storage.generatePresignedUrl(
                        verification.getDocumentUrl()
                );
            } catch (RuntimeException e) {
                log.error(
                        "인증 문서 임시 URL 생성 실패: verificationId={}",
                        verificationId,
                        e
                );

                throw new ApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "E500",
                        "인증 문서 임시 URL 생성에 실패했습니다."
                );
            }
        }

        return new AdminVerificationDetailResponse(
                verification.getId(),
                user.getName(),
                user.getRole().name(),
                getSchoolName(verification, user.getRole()),
                documentUrl,
                verification.getStatus().name(),
                formatDateTime(verification.getSubmittedAt())
        );
    }

    @Transactional
    public AdminVerificationReviewResponse review(
            String authorization,
            Long verificationId,
            AdminVerificationReviewRequest request
    ) {
        User admin = authenticateAdmin(authorization);

        if (request == null
                || request.decision() == null
                || request.decision().isBlank()) {
            throw invalidRequest(
                    "decision",
                    "승인 또는 반려 여부는 필수입니다."
            );
        }

        VerificationStatus decision;

        try {
            decision = VerificationStatus.valueOf(
                    request.decision().trim()
            );
        } catch (IllegalArgumentException e) {
            throw invalidRequest(
                    "decision",
                    "APPROVED 또는 REJECTED만 입력할 수 있습니다."
            );
        }

        if (decision != VerificationStatus.APPROVED
                && decision != VerificationStatus.REJECTED) {
            throw invalidRequest(
                    "decision",
                    "APPROVED 또는 REJECTED만 입력할 수 있습니다."
            );
        }

        String rejectReason = request.rejectReason();

        if (decision == VerificationStatus.REJECTED) {
            if (rejectReason == null
                    || rejectReason.isBlank()
                    || rejectReason.length() > 100) {
                throw invalidRequest(
                        "reject_reason",
                        "반려 사유는 1~100자로 입력해야 합니다."
                );
            }
        } else {
            if (rejectReason != null && !rejectReason.isBlank()) {
                throw invalidRequest(
                        "reject_reason",
                        "승인 시 반려 사유를 입력할 수 없습니다."
                );
            }
        }

        StudentVerification verification =
                verificationRepository.findByIdForUpdate(verificationId)
                        .orElseThrow(() -> new ApiException(
                                HttpStatus.NOT_FOUND,
                                "E404",
                                "인증 요청을 찾을 수 없습니다."
                        ));

        if (verification.getStatus() != VerificationStatus.PENDING) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "E409_ALREADY_REVIEWED",
                    "이미 심사가 끝난 요청입니다"
            );
        }

        String oldDocumentUrl = verification.getDocumentUrl();

        LocalDateTime reviewedAt = LocalDateTime.now(SEOUL);

        verification.review(
                decision,
                rejectReason,
                admin.getId(),
                reviewedAt
        );

        verificationRepository.saveAndFlush(verification);

        if (oldDocumentUrl != null) {
            registerDocumentDeletion(oldDocumentUrl, verificationId);
        }

        return new AdminVerificationReviewResponse(
                verification.getId(),
                verification.getStatus().name(),
                formatDateTime(reviewedAt)
        );
    }

    private void registerDocumentDeletion(
            String documentUrl,
            Long verificationId
    ) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {
                        try {
                            storage.delete(documentUrl);
                        } catch (Exception e) {
                            log.error(
                                    "심사 완료 후 S3 문서 삭제 실패: verificationId={}",
                                    verificationId,
                                    e
                            );
                        }
                    }
                }
        );
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

    private User authenticateAdmin(String authorization) {

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

        User admin = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "E401",
                        "사용자를 찾을 수 없습니다."
                ));

        if (admin.getRole() != UserRole.ADMIN) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "E403",
                    "운영자만 접근할 수 있습니다."
            );
        }

        return admin;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "E404",
                        "인증 요청 사용자를 찾을 수 없습니다."
                ));
    }

    private String getSchoolName(
            StudentVerification verification,
            UserRole role
    ) {
        if (role == UserRole.HIGH_SCHOOL) {
            return verification.getHighSchoolName();
        }

        if (role == UserRole.UNIVERSITY) {
            Long universityId = verification.getUniversityId();

            if (universityId == null) {
                throw new ApiException(
                        HttpStatus.NOT_FOUND,
                        "E404",
                        "대학 정보를 찾을 수 없습니다."
                );
            }

            University university = universityRepository
                    .findById(universityId)
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            "E404",
                            "대학 정보를 찾을 수 없습니다."
                    ));

            return university.getName();
        }

        throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "E400",
                "재학 인증 대상이 아닌 사용자입니다."
        );
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.atZone(SEOUL).format(FORMATTER);
    }
}