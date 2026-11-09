package com.uniro.backend.domain.course.service;

import com.uniro.backend.common.ApiException;
import com.uniro.backend.common.TourLocker;
import com.uniro.backend.common.ai.AiException;
import com.uniro.backend.domain.course.dto.CourseResponse;
import com.uniro.backend.domain.course.entity.Course;
import com.uniro.backend.domain.course.entity.CourseStatus;
import com.uniro.backend.domain.course.entity.CourseStop;
import com.uniro.backend.domain.course.repository.CourseRepository;
import com.uniro.backend.domain.course.repository.CourseRepository.TourInfo;
import com.uniro.backend.domain.course.repository.CourseStopRepository;
import com.uniro.backend.domain.course.service.CourseDraftPlanner.Candidate;
import com.uniro.backend.domain.course.service.CourseDraftPlanner.Planned;
import com.uniro.backend.domain.store.entity.Store;
import com.uniro.backend.domain.store.repository.StorePartnershipRepository;
import com.uniro.backend.domain.store.service.StoreService;
import com.uniro.backend.domain.tour.entity.Tour;
import com.uniro.backend.domain.tour.entity.TourApplicationStatus;
import com.uniro.backend.domain.tour.repository.TourApplicationRepository;
import com.uniro.backend.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

// AI 코스 초안 생성. 승인된 신청자들의 "하고 싶은 것"으로 캠퍼스 주변 상점 코스를 만든다.
// AI 호출은 느리므로 탐방 행 잠금과 DB 트랜잭션 밖에서 하고, 저장할 때만 잠그고 다시 확인한다.
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseDraftService {

    // 지점 사이 간격(분). 첫 지점은 탐방 시작 시각이다.
    private static final int STEP_MINUTES = 60;
    private static final int LAST_MINUTE_OF_DAY = 23 * 60 + 59;

    // 같은 탐방의 초안은 이 간격 안에 다시 만들 수 없다. 반복 호출로 AI 한도를 다 쓰는 것을 막는다.
    private static final long COOLDOWN_NANOS = TimeUnit.SECONDS.toNanos(10);
    private static final int COOLDOWN_CLEANUP_SIZE = 1000;

    // 같은 탐방의 초안을 동시에 두 번 만들지 않도록 진행 중인 탐방을 기억한다(AI 호출 낭비를 막는다)
    private final Set<Long> inFlight = ConcurrentHashMap.newKeySet();
    // 탐방별로 마지막으로 초안 생성을 시작한 시각(nanoTime)
    private final Map<Long, Long> lastStarted = new ConcurrentHashMap<>();

    private final CourseRepository courseRepository;
    private final CourseStopRepository stopRepository;
    private final TourApplicationRepository applicationRepository;
    private final StorePartnershipRepository partnershipRepository;
    private final StoreService storeService;
    private final CourseDraftPlanner planner;
    private final CourseAssembler assembler;
    private final TourLocker tourLocker;
    private final TransactionTemplate transactionTemplate;

    public CourseResponse create(User guide, Long tourId) {
        TourInfo info = courseRepository.findTourInfo(tourId)
                .orElseThrow(CourseErrors::tourNotFound);

        if (!info.guideId().equals(guide.getId())) {
            throw CourseErrors.notGuide("내 탐방의 코스만 만들 수 있어요");
        }
        requireOpen(info);
        requireNotConfirmed(tourId);

        List<String> wishes = applicationRepository
                .findMotivationsByTourIdAndStatus(tourId, TourApplicationStatus.APPROVED);
        if (wishes.isEmpty()) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "E409_NO_APPROVED_APPLICANT",
                    "승인된 신청자가 있어야 코스를 만들 수 있어요");
        }

        if (startedRecently(tourId)) {
            throw new ApiException(
                    HttpStatus.TOO_MANY_REQUESTS, "E429", "잠시 후 다시 시도해 주세요");
        }
        if (!inFlight.add(tourId)) {
            throw new ApiException(HttpStatus.CONFLICT, "E409", "이미 코스를 만들고 있어요");
        }

        try {
            List<Planned> planned = plan(info, wishes);

            return transactionTemplate.execute(status -> saveDraft(tourId, planned));
        } finally {
            inFlight.remove(tourId);
        }
    }

    // 쿨다운 안에 이미 시작한 요청이 있으면 true. 아니면 지금을 시작 시각으로 기록하고 false.
    private boolean startedRecently(Long tourId) {
        long now = System.nanoTime();
        boolean[] recent = {true};

        lastStarted.compute(tourId, (id, previous) -> {
            if (previous == null || now - previous >= COOLDOWN_NANOS) {
                recent[0] = false;
                return now;
            }
            return previous;
        });

        if (lastStarted.size() > COOLDOWN_CLEANUP_SIZE) {
            lastStarted.values().removeIf(started -> now - started >= COOLDOWN_NANOS);
        }

        return recent[0];
    }

    private List<Planned> plan(TourInfo info, List<String> wishes) {
        List<Store> nearest = storeService.nearbyStores(info.universityId(), null);

        Set<Long> partnerIds = nearest.isEmpty() ? Set.of()
                : new HashSet<>(partnershipRepository.findActiveStoreIds(
                        nearest.stream().map(Store::getId).toList(),
                        info.startsAt().toLocalDate()));

        List<Candidate> candidates = planner.candidates(nearest, partnerIds);
        if (candidates.isEmpty()) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "E409_NO_STORE_CANDIDATE", "캠퍼스 주변에 추천할 상점이 없어요");
        }

        try {
            return planner.plan(wishes, candidates);
        } catch (AiException e) {
            log.warn("코스 초안 생성 실패: {}", e.getMessage());
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY, "E502_AI_FAILED",
                    "코스를 만들지 못했어요. 잠시 후 다시 시도해 주세요");
        }
    }

    // 탐방 행을 잠그고, 그 사이에 바뀐 것이 없는지 다시 확인한 뒤 초안으로 바꾼다.
    private CourseResponse saveDraft(Long tourId, List<Planned> planned) {
        Tour tour = tourLocker.lock(tourId);

        if (!tour.isOpen()) {
            throw CourseErrors.tourNotOpen();
        }

        Course course = courseRepository.findById(tourId).orElse(null);
        if (course != null && course.isConfirmed()) {
            throw CourseErrors.courseConfirmed();
        }

        if (course == null) {
            course = courseRepository.save(new Course(tourId));
        } else {
            course.touch();
        }

        stopRepository.deleteAllByTourId(tourId);

        List<CourseStop> stops = new ArrayList<>();
        for (int i = 0; i < planned.size(); i++) {
            Planned p = planned.get(i);
            stops.add(CourseStop.ofStore(
                    tourId, i + 1, p.storeId(), visitTime(tour, i, planned.size()), null, p.reason()));
        }
        stopRepository.saveAll(stops);

        return assembler.assemble(tour, course, stops);
    }

    // 첫 지점은 탐방 시작 시각, 이후 60분 간격. 하루를 넘기면 간격을 줄여 자정 전에 끝낸다.
    private LocalTime visitTime(Tour tour, int index, int count) {
        int start = tour.getStartsAt().getHour() * 60 + tour.getStartsAt().getMinute();
        int step = count <= 1 ? 0
                : Math.max(1, Math.min(STEP_MINUTES, (LAST_MINUTE_OF_DAY - start) / (count - 1)));
        int minutes = Math.min(LAST_MINUTE_OF_DAY, start + step * index);

        return LocalTime.of(minutes / 60, minutes % 60);
    }

    private void requireOpen(TourInfo info) {
        if (!info.status().isOpen()) {
            throw CourseErrors.tourNotOpen();
        }
    }

    // AI를 부르기 전에 확정 여부부터 확인한다(엔티티를 읽어 두지 않는다)
    private void requireNotConfirmed(Long tourId) {
        if (courseRepository.existsByTourIdAndStatus(tourId, CourseStatus.CONFIRMED)) {
            throw CourseErrors.courseConfirmed();
        }
    }
}
