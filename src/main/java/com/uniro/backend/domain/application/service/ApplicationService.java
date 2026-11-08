package com.uniro.backend.domain.application.service;

import com.uniro.backend.common.ApiException;
import com.uniro.backend.domain.application.dto.ApplicationCreateRequest;
import com.uniro.backend.domain.application.dto.ApplicationCreateResponse;
import com.uniro.backend.domain.guardianconsent.repository.GuardianConsentRepository;
import com.uniro.backend.domain.store.support.StoreErrors;
import com.uniro.backend.domain.tour.entity.Tour;
import com.uniro.backend.domain.tour.entity.TourApplication;
import com.uniro.backend.domain.tour.entity.TourApplicationStatus;
import com.uniro.backend.domain.tour.entity.TourStatus;
import com.uniro.backend.domain.tour.repository.TourApplicationRepository;
import com.uniro.backend.domain.tour.repository.TourRepository;
import com.uniro.backend.domain.user.entity.User;
import com.uniro.backend.domain.verification.entity.VerificationStatus;
import com.uniro.backend.domain.verification.repository.StudentVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 탐방 신청
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private static final int WISH_MAX = 100;
    private static final String CONSENT_AGREED = "AGREED";

    // 유효한(아직 살아 있는) 신청 상태
    private static final List<TourApplicationStatus> ACTIVE =
            List.of(TourApplicationStatus.PENDING, TourApplicationStatus.APPROVED);

    private final TourRepository tourRepository;
    private final TourApplicationRepository applicationRepository;
    private final StudentVerificationRepository verificationRepository;
    private final GuardianConsentRepository guardianConsentRepository;

    // 탐방 신청 (고교생). 재학 인증과 보호자 동의가 끝난 고교생만 신청할 수 있다.
    @Transactional
    public ApplicationCreateResponse apply(
            User student, Long tourId, ApplicationCreateRequest request
    ) {
        String wish = normalizeWish(request.wish());

        // 마지막 자리에 두 명이 동시에 신청해도 정원을 넘지 않도록 탐방 행을 잠그고 센다.
        // 잠금은 이 트랜잭션의 첫 DB 작업이어야 한다. 다른 조회가 먼저 있으면 그 시점의 데이터가
        // 읽기 기준이 되어(REPEATABLE READ), 잠금을 기다린 뒤에도 앞 사람의 신청을 세지 못한다.
        Tour tour = tourRepository.findByIdForUpdate(tourId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "E404", "존재하지 않는 탐방입니다"));

        requireEligible(student);
        requireOpen(tour);

        if (applicationRepository.existsByTourIdAndUserIdAndStatusIn(
                tourId, student.getId(), ACTIVE)) {
            throw alreadyApplied();
        }

        try {
            return ApplicationCreateResponse.of(applicationRepository.saveAndFlush(
                    new TourApplication(tourId, student.getId(), wish)));
        } catch (DataIntegrityViolationException e) {
            // 같은 사용자가 거의 동시에 두 번 신청한 경우 (tour_id, active_user_id 유니크)
            throw alreadyApplied();
        }
    }

    // 재학 인증 승인 + 보호자 동의 완료
    private void requireEligible(User student) {
        boolean verified = verificationRepository.findById(student.getId())
                .map(v -> v.getStatus() == VerificationStatus.APPROVED)
                .orElse(false);

        boolean consented = guardianConsentRepository.findById(student.getId())
                .map(c -> CONSENT_AGREED.equals(c.getStatus()))
                .orElse(false);

        if (!verified || !consented) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "E403_VERIFICATION_REQUIRED",
                    "인증 후 신청할 수 있어요"
            );
        }
    }

    // 모집 중이고 남은 자리가 있어야 신청할 수 있다.
    // 남은 자리 = 정원 − (신청 중 + 승인). 탐방 상세의 남은 자리 계산과 같다.
    private void requireOpen(Tour tour) {
        boolean recruiting = tour.getStatus() == TourStatus.RECRUITING;
        boolean hasSeat = applicationRepository.countByTourIdAndStatusIn(
                tour.getId(), ACTIVE) < tour.getCapacity();

        if (!recruiting || !hasSeat) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "E409_TOUR_FULL", "신청할 수 없는 탐방입니다");
        }
    }

    private String normalizeWish(String wish) {
        String trimmed = wish == null ? "" : wish.trim();

        if (trimmed.isEmpty()) {
            throw StoreErrors.invalid("wish", "필수 값입니다");
        }
        if (trimmed.length() > WISH_MAX) {
            throw StoreErrors.invalid("wish", WISH_MAX + "자 이하로 입력해 주세요");
        }

        return trimmed;
    }

    private ApiException alreadyApplied() {
        return new ApiException(
                HttpStatus.CONFLICT, "E409_ALREADY_APPLIED", "이미 신청한 탐방입니다");
    }
}
