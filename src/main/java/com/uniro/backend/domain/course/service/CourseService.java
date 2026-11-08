package com.uniro.backend.domain.course.service;

import com.uniro.backend.common.ApiException;
import com.uniro.backend.common.TourLocker;
import com.uniro.backend.domain.course.dto.CourseSpotsRequest;
import com.uniro.backend.domain.course.entity.CourseStop;
import com.uniro.backend.domain.course.entity.StopType;
import com.uniro.backend.domain.store.service.StoreService;
import com.uniro.backend.domain.store.support.StoreErrors;
import com.uniro.backend.domain.tour.entity.TourStatus;
import com.uniro.backend.domain.university.entity.CampusPlace;
import com.uniro.backend.domain.university.repository.CampusPlaceRepository;
import org.springframework.transaction.support.TransactionTemplate;
import com.uniro.backend.domain.course.dto.CourseResponse;
import com.uniro.backend.domain.course.entity.Course;
import com.uniro.backend.domain.course.repository.CourseRepository;
import com.uniro.backend.domain.course.repository.CourseStopRepository;
import com.uniro.backend.domain.tour.entity.Tour;
import com.uniro.backend.domain.tour.entity.TourApplicationStatus;
import com.uniro.backend.domain.tour.repository.TourApplicationRepository;
import com.uniro.backend.domain.tour.repository.TourRepository;
import com.uniro.backend.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// 탐방 코스 조회·수정·확정
@Service
@RequiredArgsConstructor
public class CourseService {

    private final TourRepository tourRepository;
    private final TourApplicationRepository applicationRepository;
    private final CourseRepository courseRepository;
    private final CourseStopRepository stopRepository;
    private final CourseAssembler assembler;
    private final CampusPlaceRepository campusPlaceRepository;
    private final StoreService storeService;
    private final TourLocker tourLocker;
    private final TransactionTemplate transactionTemplate;

    // 코스 조회. 가이드는 초안도 볼 수 있고, 승인된 신청자는 확정된 코스만 볼 수 있다.
    @Transactional(readOnly = true)
    public CourseResponse view(User user, Long tourId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "E404", "존재하지 않는 탐방입니다"));

        boolean guide = tour.getGuideId().equals(user.getId());
        if (!guide && !applicationRepository.existsByTourIdAndUserIdAndStatusIn(
                tourId, user.getId(), List.of(TourApplicationStatus.APPROVED))) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN, "E403", "이 탐방의 코스를 볼 수 없어요");
        }

        Course course = courseRepository.findById(tourId).orElse(null);

        // 신청자에게는 없는 코스와 확정 전 코스를 구분하지 않고 준비 중이라고 알린다
        if (!guide && (course == null || !course.isConfirmed())) {
            throw notReady();
        }
        if (course == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND, "E404", "아직 만들어진 코스가 없습니다");
        }

        return assembler.assemble(tour, course, stopRepository.findByTourIdOrderByStopOrderAsc(tourId));
    }

    // 코스 수정 (해당 탐방의 가이드). 보낸 지점 전체가 새 코스가 된다.
    // 코스가 아직 없으면 보낸 지점으로 초안을 새로 만든다(AI 초안 없이도, AI가 실패해도 직접 짤 수 있다).
    public CourseResponse edit(User guide, Long tourId, CourseSpotsRequest request) {
        List<CourseSpots.Parsed> spots = CourseSpots.parse(request);

        // 권한은 탐방 행을 잠그기 전에 확인한다. 권한 없는 요청이 잠금을 잡고 다른 작업을 막지 못하게 한다.
        Long guideId = courseRepository.findGuideIdByTourId(tourId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "E404", "존재하지 않는 탐방입니다"));
        if (!guideId.equals(guide.getId())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN, "E403", "내 탐방의 코스만 수정할 수 있어요");
        }

        return transactionTemplate.execute(status -> editLocked(tourId, spots));
    }

    // 탐방 행을 잠근 상태에서 지점을 통째로 바꾼다.
    private CourseResponse editLocked(Long tourId, List<CourseSpots.Parsed> spots) {
        Tour tour = tourLocker.lock(tourId);

        if (tour.getStatus() != TourStatus.RECRUITING && tour.getStatus() != TourStatus.CLOSED) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "E409_INVALID_TOUR_STATUS", "종료되었거나 취소된 탐방입니다");
        }

        Course course = courseRepository.findById(tourId).orElse(null);
        if (course != null && course.isConfirmed()) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "E409_COURSE_CONFIRMED", "이미 확정된 코스입니다");
        }

        requireSelectable(tour, spots);

        // 같은 상점을 유지한 지점은 AI 추천 이유를 이어받는다
        Map<Long, String> reasons = new HashMap<>();
        if (course != null) {
            stopRepository.findByTourIdOrderByStopOrderAsc(tourId).stream()
                    .filter(s -> s.getStopType() == StopType.STORE
                            && s.getRecommendationReason() != null)
                    .forEach(s -> reasons.put(s.getStoreId(), s.getRecommendationReason()));
        }

        if (course == null) {
            course = courseRepository.save(new Course(tourId));
        } else {
            course.touch();
        }

        stopRepository.deleteAllByTourId(tourId);

        List<CourseStop> stops = new ArrayList<>();
        for (int i = 0; i < spots.size(); i++) {
            CourseSpots.Parsed spot = spots.get(i);
            int order = i + 1;

            stops.add(spot.type() == StopType.STORE
                    ? CourseStop.ofStore(tourId, order, spot.storeId(), spot.time(),
                            spot.comment(), reasons.get(spot.storeId()))
                    : CourseStop.ofCampus(tourId, order, spot.campusBuildingId(), spot.time(),
                            spot.comment()));
        }
        stopRepository.saveAll(stops);

        return assembler.assemble(tour, course, stops);
    }

    // 상점은 그 캠퍼스의 주변 상점에서, 캠퍼스 건물은 그 탐방 대학의 건물에서만 고를 수 있다.
    private void requireSelectable(Tour tour, List<CourseSpots.Parsed> spots) {
        List<Long> storeIds = spots.stream()
                .filter(s -> s.type() == StopType.STORE).map(CourseSpots.Parsed::storeId).toList();
        List<Long> placeIds = spots.stream()
                .filter(s -> s.type() == StopType.CAMPUS)
                .map(CourseSpots.Parsed::campusBuildingId).toList();

        Set<Long> nearby = storeIds.isEmpty() ? Set.of()
                : storeService.nearbyStoreIds(tour.getUniversityId(), storeIds);
        Set<Long> places = placeIds.isEmpty() ? Set.of()
                : campusPlaceRepository.findAllById(placeIds).stream()
                        .filter(p -> p.getUniversityId().equals(tour.getUniversityId()))
                        .map(CampusPlace::getId)
                        .collect(Collectors.toSet());

        for (int i = 0; i < spots.size(); i++) {
            CourseSpots.Parsed spot = spots.get(i);

            if (spot.type() == StopType.STORE && !nearby.contains(spot.storeId())) {
                throw StoreErrors.invalid(
                        "spots[" + i + "].store_id", "캠퍼스 주변 상점만 고를 수 있어요");
            }
            if (spot.type() == StopType.CAMPUS && !places.contains(spot.campusBuildingId())) {
                throw StoreErrors.invalid(
                        "spots[" + i + "].campus_building_id", "이 대학의 캠퍼스 건물만 고를 수 있어요");
            }
        }
    }

    private ApiException notReady() {
        return new ApiException(
                HttpStatus.CONFLICT, "E409_COURSE_NOT_READY", "가이드가 코스를 준비 중이에요");
    }
}
