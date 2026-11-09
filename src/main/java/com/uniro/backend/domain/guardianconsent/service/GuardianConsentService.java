
package com.uniro.backend.domain.guardianconsent.service;

import com.uniro.backend.common.ApiException;
import com.uniro.backend.domain.guardianconsent.dto.GuardianConsentRequest;
import com.uniro.backend.domain.guardianconsent.dto.GuardianConsentResponse;
import com.uniro.backend.domain.guardianconsent.entity.GuardianConsent;
import com.uniro.backend.domain.guardianconsent.repository.GuardianConsentRepository;
import com.uniro.backend.domain.user.entity.User;
import com.uniro.backend.domain.user.entity.UserRole;
import com.uniro.backend.domain.user.repository.UserRepository;
import com.uniro.backend.mail.GuardianMailService;
import com.uniro.backend.domain.guardianconsent.dto.GuardianConsentGuideResponse;
import com.uniro.backend.domain.guardianconsent.dto.GuardianConsentDecisionRequest;
import com.uniro.backend.domain.guardianconsent.dto.GuardianConsentDecisionResponse;

import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.time.OffsetDateTime;


@Service
public class GuardianConsentService {

    private static final ZoneId ZONE =
            ZoneId.of("Asia/Seoul");

    private final UserRepository userRepository;
    private final GuardianConsentRepository consentRepository;
    private final GuardianConsentTokenService tokenService;
    private final GuardianMailService mailService;

    public GuardianConsentService(
            UserRepository userRepository,
            GuardianConsentRepository consentRepository,
            GuardianConsentTokenService tokenService,
            GuardianMailService mailService
    ) {
        this.userRepository = userRepository;
        this.consentRepository = consentRepository;
        this.tokenService = tokenService;
        this.mailService = mailService;
    }

    @Transactional
    public GuardianConsentResponse requestConsent(
            Long userId,
            GuardianConsentRequest request
    ) {
        validateRequest(request);

        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "E404",
                        "사용자를 찾을 수 없습니다"
                ));

        if (user.getRole() != UserRole.HIGH_SCHOOL) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "E403",
                    "고등학생만 보호자 동의를 요청할 수 있습니다"
            );
        }

        LocalDateTime now = LocalDateTime.now(ZONE);

        GuardianConsent consent =
                consentRepository.findById(userId).orElse(null);

        validateCanRequest(consent, now);

        String token = tokenService.generateToken();
        String tokenHash = tokenService.hashToken(token);

        if (consent == null) {
            consent = new GuardianConsent(
                    userId,
                    request.guardianName().trim(),
                    request.guardianEmail().trim(),
                    tokenHash,
                    now
            );
        } else {
            consent.requestConsent(
                    request.guardianName().trim(),
                    request.guardianEmail().trim(),
                    tokenHash,
                    now
            );
        }

        consentRepository.saveAndFlush(consent);

        try {
            mailService.sendConsentEmail(
                    consent.getGuardianName(),
                    consent.getGuardianEmail(),
                    token
            );
        } catch (MailException e) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "E500",
                    "보호자 동의 이메일 발송에 실패했습니다"
            );
        }

        return new GuardianConsentResponse(
                "PENDING",
                consent.getExpiresAt()
                        .atZone(ZONE)
                        .toOffsetDateTime()
        );
    }

    private void validateCanRequest(
            GuardianConsent consent,
            LocalDateTime now
    ) {
        if (consent == null) {
            return;
        }

        if ("AGREED".equals(consent.getStatus())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "E409_CONSENT_EXISTS",
                    "이미 요청했거나 동의가 완료되었습니다"
            );
        }

        if ("PENDING".equals(consent.getStatus())
                && consent.getExpiresAt() != null
                && now.isBefore(consent.getExpiresAt())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "E409_CONSENT_EXISTS",
                    "이미 요청했거나 동의가 완료되었습니다"
            );
        }
    }

    private void validateRequest(GuardianConsentRequest request) {
        if (request == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "E400",
                    "요청 본문이 필요합니다"
            );
        }

        String name = request.guardianName();
        String email = request.guardianEmail();

        if (name == null || name.isBlank()
                || name.length() > 20) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "E400",
                    "보호자 이름은 1~20자로 입력해야 합니다",
                    "guardian_name",
                    "1~20자 필수"
            );
        }

        if (email == null
                || !email.matches(
                "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        )) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "E400",
                    "올바른 보호자 이메일을 입력해야 합니다",
                    "guardian_email",
                    "이메일 형식 필수"
            );
        }
    }

    @Transactional(readOnly = true)
    public GuardianConsentGuideResponse getConsentGuide(String consentToken) {

        if (consentToken == null || consentToken.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "E400",
                    "동의 토큰이 필요합니다"
            );
        }

        String tokenHash = tokenService.hashToken(consentToken);

        GuardianConsent consent = consentRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "E404",
                        "유효하지 않은 동의 링크입니다"
                ));

        LocalDateTime now = LocalDateTime.now(ZONE);

        if (consent.getExpiresAt() == null
                || !now.isBefore(consent.getExpiresAt())) {
            throw new ApiException(
                    HttpStatus.GONE,
                    "E410_EXPIRED",
                    "만료된 링크입니다. 학생에게 다시 요청해 달라고 알려주세요"
            );
        }

        User student = userRepository.findById(consent.getId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "E404",
                        "학생 정보를 찾을 수 없습니다"
                ));

        String schoolName = "학교 정보 없음";

        String guideText =
                "용용톤은 방학 중 대학생 가이드와 캠퍼스를 둘러보는 서비스입니다. "
                        + "고등학생이 대학 탐방 프로그램에 참여하기 위해서는 "
                        + "보호자의 동의가 필요합니다.";

        return new GuardianConsentGuideResponse(
                student.getName(),
                schoolName,
                guideText,
                consent.getStatus(),
                consent.getExpiresAt()
                        .atZone(ZONE)
                        .toOffsetDateTime()
        );
    }

    @Transactional
    public GuardianConsentDecisionResponse respondConsent(
            String consentToken,
            GuardianConsentDecisionRequest request
    ) {
        // 1. 요청값 검증
        if (request == null || request.agree() == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "E400",
                    "동의 여부를 입력해야 합니다",
                    "agree",
                    "필수 입력값입니다"
            );
        }

        if (consentToken == null || consentToken.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "E400",
                    "동의 토큰이 필요합니다"
            );
        }

        // 2. 토큰 해시 생성
        String tokenHash = tokenService.hashToken(consentToken);

        // 3. DB 조회 및 잠금
        GuardianConsent consent = consentRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "E404",
                        "유효하지 않은 동의 링크입니다"
                ));

        LocalDateTime now = LocalDateTime.now(ZONE);

        // 4. 이미 처리된 요청인지 확인
        if (!"PENDING".equals(consent.getStatus())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "E409_ALREADY_PROCESSED",
                    "이미 처리된 요청입니다"
            );
        }

        // 5. 만료 여부 확인
        if (consent.getExpiresAt() == null
                || !now.isBefore(consent.getExpiresAt())) {
            throw new ApiException(
                    HttpStatus.GONE,
                    "E410_EXPIRED",
                    "만료된 링크입니다"
            );
        }

        // 6. 동의 또는 거부 처리
        consent.respond(request.agree(), now);

        return new GuardianConsentDecisionResponse(
                consent.getStatus()
        );
    }
}
