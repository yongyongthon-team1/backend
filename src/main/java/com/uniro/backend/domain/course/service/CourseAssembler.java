package com.uniro.backend.domain.course.service;

import com.uniro.backend.domain.course.dto.CourseResponse;
import com.uniro.backend.domain.course.entity.Course;
import com.uniro.backend.domain.course.entity.CourseStop;
import com.uniro.backend.domain.course.entity.StopType;
import com.uniro.backend.domain.review.entity.StoreReviewSummary;
import com.uniro.backend.domain.review.repository.StoreReviewSummaryRepository;
import com.uniro.backend.domain.store.entity.Store;
import com.uniro.backend.domain.store.repository.StorePartnershipRepository;
import com.uniro.backend.domain.store.repository.StoreRepository;
import com.uniro.backend.domain.tour.entity.Tour;
import com.uniro.backend.domain.university.entity.CampusPlace;
import com.uniro.backend.domain.university.repository.CampusPlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

// 코스와 지점을 지도 핀 정보(응답)로 바꾼다. 조회와 수정이 같은 응답 형식을 쓴다.
// 상점·캠퍼스 건물·제휴·리뷰 요약은 지점마다 따로 묻지 않고 한 번에 가져온다.
@Component
@RequiredArgsConstructor
public class CourseAssembler {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final StoreRepository storeRepository;
    private final CampusPlaceRepository campusPlaceRepository;
    private final StorePartnershipRepository partnershipRepository;
    private final StoreReviewSummaryRepository summaryRepository;

    public CourseResponse assemble(Tour tour, Course course, List<CourseStop> stops) {
        List<Long> storeIds = stops.stream()
                .filter(s -> s.getStopType() == StopType.STORE)
                .map(CourseStop::getStoreId)
                .toList();
        List<Long> placeIds = stops.stream()
                .filter(s -> s.getStopType() == StopType.CAMPUS)
                .map(CourseStop::getCampusBuildingId)
                .toList();

        Map<Long, Store> stores = storeIds.isEmpty() ? Map.of()
                : storeRepository.findAllById(storeIds).stream()
                        .collect(Collectors.toMap(Store::getId, Function.identity()));
        Map<Long, CampusPlace> places = placeIds.isEmpty() ? Map.of()
                : campusPlaceRepository.findAllById(placeIds).stream()
                        .collect(Collectors.toMap(CampusPlace::getId, Function.identity()));
        Map<Long, String> summaries = storeIds.isEmpty() ? Map.of()
                : summaryRepository.findByStoreIdIn(storeIds).stream()
                        .collect(Collectors.toMap(
                                StoreReviewSummary::getStoreId, StoreReviewSummary::getSummary));
        Set<Long> partners = partnerStoreIds(tour, course, stops, storeIds);

        return new CourseResponse(
                course.getTourId(),
                course.getStatus(),
                stops.stream().map(s -> spot(s, stores, places, summaries, partners)).toList()
        );
    }

    // 확정된 코스는 확정 때 기록한 제휴 행사로, 초안은 탐방 날짜에 제휴 기간 안인 상점으로 정한다.
    private Set<Long> partnerStoreIds(
            Tour tour, Course course, List<CourseStop> stops, List<Long> storeIds
    ) {
        if (course.isConfirmed()) {
            return stops.stream()
                    .filter(s -> s.getPartnershipId() != null)
                    .map(CourseStop::getStoreId)
                    .collect(Collectors.toSet());
        }
        if (storeIds.isEmpty()) {
            return Set.of();
        }

        return new HashSet<>(partnershipRepository.findActiveStoreIds(
                storeIds, tour.getStartsAt().toLocalDate()));
    }

    private CourseResponse.Spot spot(
            CourseStop stop,
            Map<Long, Store> stores,
            Map<Long, CampusPlace> places,
            Map<Long, String> summaries,
            Set<Long> partners
    ) {
        String time = stop.getVisitTime() == null ? null : stop.getVisitTime().format(TIME);

        if (stop.getStopType() == StopType.STORE) {
            Store store = stores.get(stop.getStoreId());

            return new CourseResponse.Spot(
                    stop.getStopOrder(), StopType.STORE, stop.getStoreId(), null,
                    store == null ? null : store.getName(),
                    store == null ? null : store.getCategory(),
                    store == null ? null : store.getAddress(),
                    store == null ? null : store.getLatitude(),
                    store == null ? null : store.getLongitude(),
                    time, stop.getRecommendationReason(), stop.getDescription(),
                    partners.contains(stop.getStoreId()),
                    summaries.get(stop.getStoreId())
            );
        }

        CampusPlace place = places.get(stop.getCampusBuildingId());

        return new CourseResponse.Spot(
                stop.getStopOrder(), StopType.CAMPUS, null, stop.getCampusBuildingId(),
                place == null ? null : place.getName(),
                null,
                null,
                place == null ? null : place.getLatitude(),
                place == null ? null : place.getLongitude(),
                time, stop.getRecommendationReason(), stop.getDescription(),
                false,
                null
        );
    }
}
