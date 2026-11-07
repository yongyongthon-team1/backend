package com.uniro.backend.domain.verification.service;

import com.uniro.backend.common.ApiException;
import com.uniro.backend.config.JwtProvider;
import com.uniro.backend.domain.user.entity.User;
import com.uniro.backend.domain.user.entity.UserRole;
import com.uniro.backend.domain.user.repository.UserRepository;
import com.uniro.backend.domain.university.repository.UniversityRepository;
import com.uniro.backend.domain.verification.dto.EnrollmentVerificationResponse;
import com.uniro.backend.domain.verification.entity.StudentVerification;
import com.uniro.backend.domain.verification.entity.VerificationStatus;
import com.uniro.backend.domain.verification.repository.StudentVerificationRepository;
import com.uniro.backend.storage.S3DocumentStorage;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnrollmentVerificationService {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final String FILE_ERROR =
            "jpg, png 형식의 5MB 이하 파일만 올릴 수 있어요";

    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;
    private final StudentVerificationRepository verificationRepository;
    private final S3DocumentStorage storage;
    private final JwtProvider jwtProvider;
    private final TransactionTemplate transactionTemplate;

    @Value("${verification.enrollment.allowed-university-ids}")
    private String allowedUniversityIds;

    public EnrollmentVerificationResponse submit(
            String authorization,
            Long campusId,
            String schoolName,
            MultipartFile document
    ) {
        Long userId = authenticate(authorization);

        validateDocument(document);

        // DB 트랜잭션 내에서 사용자 잠금과 인증 상태 확인
        // S3 업로드 이후 DB 실패 시 보상 삭제 수행
        return transactionTemplate.execute(status -> {

            User user = userRepository.findByIdForUpdate(userId)
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.UNAUTHORIZED,
                            "E401",
                            "인증이 필요합니다"
                    ));

            validateRoleAndFields(user, campusId, schoolName);

            StudentVerification existing =
                    verificationRepository.findById(userId)
                            .orElse(null);

            if (existing != null &&
                    existing.getStatus() != VerificationStatus.REJECTED) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "E409_ALREADY_SUBMITTED",
                        "이미 심사 중이거나 승인된 인증입니다"
                );
            }

            String oldDocumentUrl =
                    existing == null ? null : existing.getDocumentUrl();

            String contentType = detectImageType(document);
            String newDocumentUrl;

            try {
                newDocumentUrl = storage.upload(
                        userId,
                        document,
                        contentType
                );
            } catch (IOException | SdkException e) {
                throw new ApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "E500",
                        "인증 파일 업로드에 실패했습니다"
                );
            }

            try {
                StudentVerification verification;

                if (existing == null) {
                    verification = new StudentVerification(
                            userId,
                            user.getRole() == UserRole.HIGH_SCHOOL
                                    ? schoolName : null,
                            user.getRole() == UserRole.UNIVERSITY
                                    ? campusId : null,
                            newDocumentUrl
                    );
                } else {
                    verification = existing;
                    verification.resubmit(
                            user.getRole() == UserRole.HIGH_SCHOOL
                                    ? schoolName : null,
                            user.getRole() == UserRole.UNIVERSITY
                                    ? campusId : null,
                            newDocumentUrl
                    );
                }

                verificationRepository.saveAndFlush(verification);

                // 커밋 이후 이전 파일 삭제 및 롤백 시 새 파일 삭제는
                // 아래의 트랜잭션 동기화 콜백에서 처리
                registerStorageCleanup(newDocumentUrl, oldDocumentUrl);

                OffsetDateTime submittedAt =
                        verification.getSubmittedAt()
                                .atZone(ZoneId.of("Asia/Seoul"))
                                .toOffsetDateTime();

                return new EnrollmentVerificationResponse(
                        verification.getId(),
                        verification.getStatus(),
                        submittedAt
                );

            } catch (RuntimeException e) {
                // 동기화 콜백 등록 전 발생한 DB 오류
                try {
                    storage.delete(newDocumentUrl);
                } catch (Exception cleanupError) {
                    e.addSuppressed(cleanupError);
                }
                throw e;
            }
        });
    }

    private Long authenticate(String authorization) {
        if (authorization == null ||
                !authorization.startsWith("Bearer ")) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "E401",
                    "인증이 필요합니다"
            );
        }

        try {
            return jwtProvider.getUserId(
                    authorization.substring(7).trim()
            );
        } catch (JwtException | IllegalArgumentException e) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "E401",
                    "유효하지 않거나 만료된 토큰입니다"
            );
        }
    }

    private void validateRoleAndFields(
            User user,
            Long campusId,
            String schoolName
    ) {
        if (user.getRole() != UserRole.HIGH_SCHOOL &&
                user.getRole() != UserRole.UNIVERSITY) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "E403",
                    "학생만 재학 인증을 제출할 수 있습니다"
            );
        }

        if (user.getRole() == UserRole.HIGH_SCHOOL) {
            if (schoolName == null ||
                    schoolName.isBlank() ||
                    schoolName.length() > 50) {
                throw invalidField(
                        "school_name",
                        "학교명은 1~50자로 입력해야 합니다"
                );
            }

            if (campusId != null) {
                throw invalidField(
                        "campus_id",
                        "고교생은 campus_id를 입력할 수 없습니다"
                );
            }
        }

        if (user.getRole() == UserRole.UNIVERSITY) {
            if (campusId == null) {
                throw invalidField(
                        "campus_id",
                        "대학생은 campus_id가 필수입니다"
                );
            }

            if (schoolName != null) {
                throw invalidField(
                        "school_name",
                        "대학생은 school_name을 입력할 수 없습니다"
                );
            }

            Set<Long> allowedIds;
            try {
                allowedIds = Arrays.stream(
                                allowedUniversityIds.split(",")
                        )
                        .map(String::trim)
                        .map(Long::parseLong)
                        .collect(Collectors.toSet());
            } catch (NumberFormatException e) {
                throw new IllegalStateException(
                        "ENROLLMENT_UNIVERSITY_IDS 설정이 잘못되었습니다",
                        e
                );
            }

            if (allowedIds.size() != 5) {
                throw new IllegalStateException(
                        "참여 대학 ID는 정확히 5개여야 합니다"
                );
            }

            if (!allowedIds.contains(campusId) ||
                    !universityRepository.existsById(campusId)) {
                throw invalidField(
                        "campus_id",
                        "참여 대학 5개 중 하나를 선택해야 합니다"
                );
            }
        }
    }

    private void validateDocument(MultipartFile document) {
        if (document == null ||
                document.isEmpty() ||
                document.getSize() > MAX_FILE_SIZE) {
            throw invalidField("document", FILE_ERROR);
        }

        detectImageType(document);
    }

    private String detectImageType(MultipartFile document) {
        try {
            byte[] header = document.getInputStream().readNBytes(8);

            boolean jpeg = header.length >= 3 &&
                    (header[0] & 0xff) == 0xff &&
                    (header[1] & 0xff) == 0xd8 &&
                    (header[2] & 0xff) == 0xff;

            boolean png = header.length == 8 &&
                    Arrays.equals(header, new byte[]{
                            (byte) 137, 80, 78, 71, 13, 10, 26, 10
                    });

            if (jpeg) return "image/jpeg";
            if (png) return "image/png";

            throw invalidField("document", FILE_ERROR);

        } catch (IOException e) {
            throw invalidField("document", FILE_ERROR);
        }
    }

    private ApiException invalidField(
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

    private void registerStorageCleanup(
            String newDocumentUrl,
            String oldDocumentUrl
    ) {
        org.springframework.transaction.support.TransactionSynchronizationManager
                .registerSynchronization(
                        new org.springframework.transaction.support.TransactionSynchronization() {

                            @Override
                            public void afterCompletion(int status) {
                                if (status ==
                                        STATUS_ROLLED_BACK) {
                                    try {
                                        storage.delete(newDocumentUrl);
                                    } catch (Exception e) {
                                        // 운영 환경에서는 로그와 재시도 작업으로 처리
                                    }
                                }

                                if (status ==
                                        STATUS_COMMITTED &&
                                        oldDocumentUrl != null) {
                                    try {
                                        storage.delete(oldDocumentUrl);
                                    } catch (Exception e) {
                                        // 운영 환경에서는 로그와 재시도 작업으로 처리
                                    }
                                }
                            }
                        }
                );
    }
}