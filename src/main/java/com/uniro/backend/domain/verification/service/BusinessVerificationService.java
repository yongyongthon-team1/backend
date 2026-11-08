package com.uniro.backend.domain.verification.service;

import com.uniro.backend.common.ApiException;
import com.uniro.backend.config.JwtProvider;
import com.uniro.backend.domain.user.entity.User;
import com.uniro.backend.domain.user.entity.UserRole;
import com.uniro.backend.domain.user.repository.UserRepository;
import com.uniro.backend.domain.verification.client.NtsApiException;
import com.uniro.backend.domain.verification.client.NtsBusinessClient;
import com.uniro.backend.domain.verification.dto.BusinessVerificationRequest;
import com.uniro.backend.domain.verification.dto.BusinessVerificationResponse;
import com.uniro.backend.domain.verification.entity.BusinessVerification;
import com.uniro.backend.domain.verification.repository.BusinessVerificationRepository;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

@Service
@RequiredArgsConstructor
public class BusinessVerificationService {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final BusinessVerificationRepository verificationRepository;
    private final NtsBusinessClient ntsBusinessClient;

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final org.springframework.transaction.support.TransactionTemplate
            transactionTemplate;

    public BusinessVerificationResponse verify(
            String authorization,
            BusinessVerificationRequest request
    ) {
        Long userId = getUserId(authorization);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "E404",
                        "사용자를 찾을 수 없습니다"
                ));

        if (user.getRole() != UserRole.OWNER) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "E403",
                    "사장님 계정만 사업자 인증을 진행할 수 있습니다"
            );
        }

        validateRequest(request);

        if (!ntsBusinessClient.isMatching(
                request.businessNumber(),
                request.representativeName().trim(),
                request.openedAt().replace("-", "")
        )) {
            throw invalidBusiness("MISMATCH");
        }

        if (!ntsBusinessClient.isActive(request.businessNumber())) {
            throw invalidBusiness("CLOSED");
        }

        transactionTemplate.executeWithoutResult(status -> saveApproval(userId));

        return new BusinessVerificationResponse("APPROVED");
    }

    public void saveApproval(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "E404",
                        "사용자를 찾을 수 없습니다"
                ));

        if (user.getRole() != UserRole.OWNER) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "E403",
                    "사장님 계정만 사업자 인증을 진행할 수 있습니다"
            );
        }

        LocalDateTime now = LocalDateTime.now(ZONE);

        BusinessVerification verification =
                verificationRepository.findById(userId).orElse(null);

        if (verification == null) {
            verification = new BusinessVerification(userId, now);
        } else {
            verification.approve(now);
        }

        verificationRepository.save(verification);
    }

    private Long getUserId(String authorization) {
        if (authorization == null
                || !authorization.startsWith("Bearer ")) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "E401",
                    "인증 토큰이 필요합니다"
            );
        }

        try {
            return jwtProvider.getUserId(
                    authorization.substring(7)
            );
        } catch (JwtException | IllegalArgumentException e) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "E401",
                    "유효하지 않거나 만료된 토큰입니다"
            );
        }
    }

    private void validateRequest(BusinessVerificationRequest request) {
        if (request == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "E400",
                    "요청 본문이 필요합니다"
            );
        }

        if (request.businessNumber() == null
                || !request.businessNumber().matches("\\d{10}")) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "E400",
                    "사업자등록번호는 숫자 10자리여야 합니다",
                    "business_number",
                    "숫자 10자리 필수"
            );
        }

        if (request.representativeName() == null
                || request.representativeName().isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "E400",
                    "대표자명을 입력해야 합니다",
                    "representative_name",
                    "필수 입력값입니다"
            );
        }

        try {
            LocalDate openedAt = LocalDate.parse(request.openedAt());

            if (openedAt.isAfter(LocalDate.now(ZONE))) {
                throw new DateTimeParseException(
                        "미래 날짜", request.openedAt(), 0
                );
            }
        } catch (DateTimeParseException | NullPointerException e) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "E400",
                    "개업일자를 YYYY-MM-DD 형식으로 입력해야 합니다",
                    "opened_at",
                    "유효한 개업일자 필수"
            );
        }
    }

    private ApiException invalidBusiness(String reason) {
        return new ApiException(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "E422_BUSINESS_INVALID",
                "사업자 정보를 확인할 수 없습니다",
                "business_number",
                reason
        );
    }
}