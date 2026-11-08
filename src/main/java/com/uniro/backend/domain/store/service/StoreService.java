package com.uniro.backend.domain.store.service;

import com.uniro.backend.common.ApiException;
import com.uniro.backend.domain.store.dto.NearbyStoreResponse;
import com.uniro.backend.domain.store.dto.StoreSearchItemResponse;
import com.uniro.backend.domain.store.entity.Store;
import com.uniro.backend.domain.store.repository.StorePartnershipRepository;
import com.uniro.backend.domain.store.repository.StoreRepository;
import com.uniro.backend.domain.store.support.Geo;
import com.uniro.backend.domain.store.support.StoreErrors;
import com.uniro.backend.domain.university.entity.University;
import com.uniro.backend.domain.university.repository.UniversityRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;
    private static final int MAX_KEYWORD_LENGTH = 100;

    private static final int MAX_RADIUS_METERS = 50_000;
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final StoreRepository storeRepository;
    private final StorePartnershipRepository partnershipRepository;
    private final UniversityRepository universityRepository;

    // 주변 상점 반경(미터). 팀에서 정하기 전까지 서버 기본값을 쓴다.
    @Value("${store.nearby.radius-meters:1500}")
    private int radiusMeters;

    // 잘못된 설정(0, 음수, 너무 큰 값)이면 조용히 빈 목록이나 전체 목록을 주지 않고 시작 단계에서 실패시킨다
    @PostConstruct
    void validateRadius() {
        if (radiusMeters < 1 || radiusMeters > MAX_RADIUS_METERS) {
            throw new IllegalStateException(
                    "store.nearby.radius-meters는 1~" + MAX_RADIUS_METERS
                            + " 사이여야 합니다: " + radiusMeters);
        }
    }

    // 상호로 상점 검색 (사장님이 내 가게를 연결할 때 사용)
    public Map<String, Object> search(String keyword, Integer page, Integer size) {
        String trimmed = keyword == null ? "" : keyword.trim();

        if (trimmed.length() < 2) {
            throw StoreErrors.invalid("keyword", "검색어는 2자 이상 입력해 주세요");
        }

        if (trimmed.length() > MAX_KEYWORD_LENGTH) {
            throw StoreErrors.invalid(
                    "keyword",
                    "검색어는 " + MAX_KEYWORD_LENGTH + "자 이하로 입력해 주세요"
            );
        }

        int pageNo = page == null ? 1 : page;
        int pageSize = size == null ? DEFAULT_SIZE : size;

        if (pageNo < 1) {
            throw StoreErrors.invalid("page", "page는 1 이상이어야 합니다");
        }
        if (pageSize < 1 || pageSize > MAX_SIZE) {
            throw StoreErrors.invalid("size", "size는 1~" + MAX_SIZE + " 사이여야 합니다");
        }

        Page<Store> result = storeRepository.findByNameContaining(
                trimmed,
                PageRequest.of(pageNo - 1, pageSize, Sort.by("name", "id"))
        );

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", result.getContent().stream()
                .map(s -> new StoreSearchItemResponse(
                        s.getId(),
                        s.getName(),
                        s.getAddress(),
                        s.getCategory(),
                        s.getOwnerId() != null
                ))
                .toList());
        data.put("page", pageNo);
        data.put("size", pageSize);
        data.put("total", result.getTotalElements());
        return data;
    }

    // 캠퍼스 주변 상점 (가까운 순). 제휴 여부는 오늘이 제휴 기간 안일 때만 true.
    public List<NearbyStoreResponse> nearby(Long campusId, String category) {
        if (campusId == null) {
            throw StoreErrors.invalid("campus_id", "필수 값입니다");
        }

        University campus = universityRepository.findById(campusId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "E404", "존재하지 않는 캠퍼스입니다"));

        String categoryFilter =
                category == null || category.isBlank() ? null : category.trim();

        Geo.Origin origin = Geo.origin(
                campus.getLatitude().doubleValue(),
                campus.getLongitude().doubleValue()
        );

        // 반경을 감싸는 사각형으로 먼저 줄이고, 정확한 거리는 아래에서 거른다
        Geo.Box box = origin.box(radiusMeters);

        List<Store> inBox = storeRepository.findInBox(
                BigDecimal.valueOf(box.minLat()),
                BigDecimal.valueOf(box.maxLat()),
                BigDecimal.valueOf(box.minLng()),
                BigDecimal.valueOf(box.maxLng()),
                categoryFilter
        );

        // 거리는 상점마다 한 번만 계산하고, 가까운 순으로 정렬한 뒤 상점만 남긴다
        List<Store> stores = inBox.stream()
                .map(s -> new StoreDistance(s, origin.distanceTo(
                        s.getLatitude().doubleValue(),
                        s.getLongitude().doubleValue())))
                .filter(d -> d.meters() <= radiusMeters)
                .sorted(Comparator
                        .comparingDouble(StoreDistance::meters)
                        .thenComparingLong(d -> d.store().getId()))
                .map(StoreDistance::store)
                .toList();

        if (stores.isEmpty()) {
            return List.of();
        }

        Set<Long> partnerIds = new HashSet<>(partnershipRepository.findActiveStoreIds(
                stores.stream().map(Store::getId).toList(),
                LocalDate.now(SEOUL)
        ));

        return stores.stream()
                .map(s -> NearbyStoreResponse.of(s, partnerIds.contains(s.getId())))
                .toList();
    }

    // 상점과 기준점까지의 거리(미터)
    private record StoreDistance(Store store, double meters) {
    }
}
