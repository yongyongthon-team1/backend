package com.uniro.backend.domain.application.service;

import com.uniro.backend.common.ApiException;
import com.uniro.backend.domain.application.dto.ApplicantListResponse;
import com.uniro.backend.domain.application.dto.ApplicationCancelResponse;
import com.uniro.backend.domain.application.dto.ApplicationCreateRequest;
import com.uniro.backend.domain.application.dto.ApplicationCreateResponse;
import com.uniro.backend.domain.application.dto.ApplicationDecisionRequest;
import com.uniro.backend.domain.application.dto.ApplicationDecisionResponse;
import com.uniro.backend.common.Kst;
import com.uniro.backend.common.TourLocker;
import com.uniro.backend.domain.coupon.service.CouponService;
import com.uniro.backend.domain.course.service.CourseService;
import com.uniro.backend.domain.guardianconsent.repository.GuardianConsentRepository;
import com.uniro.backend.domain.store.support.StoreErrors;
import com.uniro.backend.domain.tour.entity.Tour;
import com.uniro.backend.domain.tour.entity.TourApplication;
import com.uniro.backend.domain.tour.entity.TourApplicationStatus;
import com.uniro.backend.domain.tour.entity.TourStatus;
import com.uniro.backend.domain.tour.repository.TourApplicationRepository;
import com.uniro.backend.domain.tour.repository.TourApplicationRepository.ApplicationRef;
import com.uniro.backend.domain.tour.repository.TourRepository;
import com.uniro.backend.domain.user.entity.User;
import com.uniro.backend.domain.verification.entity.VerificationStatus;
import com.uniro.backend.domain.verification.repository.StudentVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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
    private final CouponService couponService;
    private final TransactionTemplate transactionTemplate;
    private final TourLocker tourLocker;
    private final CourseService courseService;

    // 탐방 신청 (고교생). 재학 인증과 보호자 동의가 끝난 고교생만 신청할 수 있다.
    @Transactional
    public ApplicationCreateResponse apply(
            User student, Long tourId, ApplicationCreateRequest request
    ) {
        String wish = normalizeWish(request.wish());

        // 마지막 자리에 두 명이 동시에 신청해도 정원을 넘지 않도록 탐방 행을 잠그고 센다.
        // 잠금은 이 트랜잭션의 첫 DB 작업이어야 한다. 다른 조회가 먼저 있으면 그 시점의 데이터가
        // 읽기 기준이 되어(REPEATABLE READ), 잠금을 기다린 뒤에도 앞 사람의 신청을 세지 못한다.
        Tour tour = tourLocker.lock(tourId);

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

    // 신청자 목록 (해당 탐방의 가이드). 신청 시각이 빠른 순이고, status로 거를 수 있다.
    @Transactional(readOnly = true)
    public ApplicantListResponse applicants(
            User guide, Long tourId, TourApplicationStatus status
    ) {
        Tour tour = tourRepository.findById(tourId).orElseThrow(this::tourNotFound);

        if (!tour.getGuideId().equals(guide.getId())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN, "E403", "내 탐방의 신청자만 볼 수 있어요");
        }

        return ApplicantListResponse.of(applicationRepository.findApplicants(tourId, status));
    }

    // 신청 승인·거절 (해당 탐방의 가이드). 승인 인원이 정원이 되면 탐방을 마감한다.
    public ApplicationDecisionResponse decide(
            User guide, Long applicationId, ApplicationDecisionRequest request
    ) {
        TourApplicationStatus decision = parseDecision(request.decision());

        // 탐방 행 잠금이 트랜잭션의 첫 DB 작업이 되도록, 탐방 ID와 권한은 트랜잭션 밖에서 먼저 확인한다.
        // (권한 없는 요청이 탐방 행 잠금을 잡고 다른 신청을 막지 못하게 하는 효과도 있다.)
        ApplicationRef ref = findRef(applicationId);
        if (!ref.guideId().equals(guide.getId())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN, "E403", "내 탐방의 신청만 처리할 수 있어요");
        }

        return transactionTemplate.execute(status ->
                decideLocked(applicationId, ref.tourId(), decision));
    }

    // 탐방 행을 잠근 상태에서 승인·거절을 처리한다.
    private ApplicationDecisionResponse decideLocked(
            Long applicationId, Long tourId, TourApplicationStatus decision
    ) {
        Tour tour = tourLocker.lock(tourId);
        TourApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(this::applicationNotFound);

        if (!tour.isOpen()) {
            throw alreadyProcessed("종료되었거나 취소된 탐방입니다");
        }
        if (application.getStatus() != TourApplicationStatus.PENDING) {
            throw alreadyProcessed("이미 처리된 신청입니다");
        }

        if (decision == TourApplicationStatus.REJECTED) {
            application.reject();
        } else {
            long approved = applicationRepository.countByTourIdAndStatus(
                    tourId, TourApplicationStatus.APPROVED);
            if (approved >= tour.getCapacity()) {
                throw new ApiException(
                        HttpStatus.CONFLICT, "E409_TOUR_FULL", "정원이 모두 찼습니다");
            }

            application.approve();
            // 코스가 이미 확정된 탐방이면 새로 승인된 신청자도 쿠폰을 받는다
            courseService.issueCouponsIfConfirmed(tour, applicationId);
            if (approved + 1 >= tour.getCapacity()) {
                tour.close();
            }
        }

        return new ApplicationDecisionResponse(
                application.getId(), application.getStatus(), tour.getStatus());
    }

    private ApiException alreadyProcessed(String message) {
        return new ApiException(HttpStatus.CONFLICT, "E409_ALREADY_PROCESSED", message);
    }

    // 신청 취소 (신청한 고교생). 승인됐던 신청이면 자리를 돌려주고, 이미 발급된 쿠폰은 만료시킨다.
    public ApplicationCancelResponse cancel(User student, Long applicationId) {
        // decide()와 같은 이유로 탐방 ID와 권한을 트랜잭션 밖에서 먼저 확인한다.
        ApplicationRef ref = findRef(applicationId);
        if (!ref.applicantId().equals(student.getId())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN, "E403", "내 신청만 취소할 수 있어요");
        }

        return transactionTemplate.execute(status ->
                cancelLocked(applicationId, ref.tourId()));
    }

    private ApplicationCancelResponse cancelLocked(Long applicationId, Long tourId) {
        Tour tour = tourLocker.lock(tourId);

        TourApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(this::applicationNotFound);

        if (!tour.isOpen() || !ACTIVE.contains(application.getStatus())) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "E409_INVALID_APPLICATION_STATUS", "취소할 수 없는 신청입니다");
        }

        boolean wasApproved = application.getStatus() == TourApplicationStatus.APPROVED;
        application.cancel();

        if (wasApproved) {
            // 이미 시작한 탐방은 자리가 나도 다시 모집하지 않는다
            if (tour.getStartsAt().isAfter(Kst.now())) {
                tour.reopen();
            }
            couponService.expireIssuedCoupons(List.of(applicationId));
        }

        return new ApplicationCancelResponse(application.getId(), application.getStatus());
    }

    private TourApplicationStatus parseDecision(String decision) {
        if ("APPROVED".equals(decision)) {
            return TourApplicationStatus.APPROVED;
        }
        if ("REJECTED".equals(decision)) {
            return TourApplicationStatus.REJECTED;
        }

        throw StoreErrors.invalid("decision", "APPROVED 또는 REJECTED만 가능합니다");
    }

    private ApplicationRef findRef(Long applicationId) {
        return applicationRepository.findRefById(applicationId)
                .orElseThrow(this::applicationNotFound);
    }

    private ApiException tourNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "E404", "존재하지 않는 탐방입니다");
    }

    private ApiException applicationNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "E404", "존재하지 않는 신청입니다");
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
