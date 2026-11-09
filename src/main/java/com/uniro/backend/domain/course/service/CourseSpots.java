package com.uniro.backend.domain.course.service;

import com.uniro.backend.domain.course.dto.CourseSpotsRequest;
import com.uniro.backend.domain.course.entity.StopType;
import com.uniro.backend.domain.store.support.StoreErrors;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

// 코스 수정 요청의 지점 목록을 검사해 값으로 바꾼다. DB를 읽지 않는 검사만 한다.
final class CourseSpots {

    static final int MAX_SPOTS = 20;
    static final int COMMENT_MAX = 100;

    private static final Pattern TIME = Pattern.compile("([01]\\d|2[0-3]):[0-5]\\d");

    private CourseSpots() {
    }

    record Parsed(StopType type, Long storeId, Long campusBuildingId, LocalTime time, String comment) {
    }

    static List<Parsed> parse(CourseSpotsRequest request) {
        List<CourseSpotsRequest.Spot> spots = request == null ? null : request.spots();

        if (spots == null || spots.isEmpty()) {
            throw StoreErrors.invalid("spots", "지점을 1개 이상 보내 주세요");
        }
        if (spots.size() > MAX_SPOTS) {
            throw StoreErrors.invalid("spots", "지점은 " + MAX_SPOTS + "개 이하여야 합니다");
        }

        List<Parsed> parsed = new ArrayList<>();
        Set<Long> storeIds = new HashSet<>();
        Set<Long> buildingIds = new HashSet<>();
        LocalTime previous = null;

        for (int i = 0; i < spots.size(); i++) {
            CourseSpotsRequest.Spot spot = spots.get(i);
            String field = "spots[" + i + "]";

            if (spot == null) {
                throw StoreErrors.invalid(field, "필수 값입니다");
            }

            StopType type = type(spot.stopType(), field);
            if (type == StopType.STORE) {
                if (spot.storeId() == null) {
                    throw StoreErrors.invalid(field + ".store_id", "필수 값입니다");
                }
                // 같은 상점이 두 번 들어가면 상점마다 하나인 쿠폰이 겹친다
                if (!storeIds.add(spot.storeId())) {
                    throw StoreErrors.invalid(field + ".store_id", "같은 상점을 두 번 넣을 수 없어요");
                }
            } else {
                if (spot.campusBuildingId() == null) {
                    throw StoreErrors.invalid(field + ".campus_building_id", "필수 값입니다");
                }
                if (!buildingIds.add(spot.campusBuildingId())) {
                    throw StoreErrors.invalid(
                            field + ".campus_building_id", "같은 건물을 두 번 넣을 수 없어요");
                }
            }

            // 배열 순서가 방문 순서이므로 시각도 앞 지점보다 늦어야 한다
            LocalTime time = time(spot.startTime(), field);
            if (previous != null && !time.isAfter(previous)) {
                throw StoreErrors.invalid(
                        field + ".start_time", "앞 지점보다 늦은 시각이어야 해요");
            }
            previous = time;

            parsed.add(new Parsed(
                    type,
                    type == StopType.STORE ? spot.storeId() : null,
                    type == StopType.CAMPUS ? spot.campusBuildingId() : null,
                    time,
                    comment(spot.comment(), field)
            ));
        }

        return parsed;
    }

    private static StopType type(String value, String field) {
        if ("STORE".equals(value)) {
            return StopType.STORE;
        }
        if ("CAMPUS".equals(value)) {
            return StopType.CAMPUS;
        }

        throw StoreErrors.invalid(field + ".stop_type", "STORE 또는 CAMPUS만 가능합니다");
    }

    private static LocalTime time(String value, String field) {
        if (value == null || !TIME.matcher(value.trim()).matches()) {
            throw StoreErrors.invalid(field + ".start_time", "HH:mm 형식으로 보내 주세요");
        }

        return LocalTime.parse(value.trim());
    }

    private static String comment(String value, String field) {
        String trimmed = value == null ? "" : value.trim();

        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > COMMENT_MAX) {
            throw StoreErrors.invalid(field + ".comment", COMMENT_MAX + "자 이하로 입력해 주세요");
        }

        return trimmed;
    }
}
