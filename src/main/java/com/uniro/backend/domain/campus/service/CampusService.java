package com.uniro.backend.domain.campus.service;

import com.uniro.backend.common.ApiException;
import com.uniro.backend.domain.campus.dto.CampusPlaceResponse;
import com.uniro.backend.domain.campus.dto.CampusResponse;
import com.uniro.backend.domain.university.entity.UniversityVacation;
import com.uniro.backend.domain.university.repository.CampusPlaceRepository;
import com.uniro.backend.domain.university.repository.UniversityRepository;
import com.uniro.backend.domain.university.repository.UniversityVacationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampusService {

    private final UniversityRepository universityRepository;
    private final UniversityVacationRepository vacationRepository;
    private final CampusPlaceRepository placeRepository;

    // 재학 인증과 같은 5개교 제한
    @Value("${verification.enrollment.allowed-university-ids}")
    private String allowedUniversityIds;

    public List<CampusResponse> getCampuses() {
        Set<Long> allowed = allowedIds();

        Map<Long, List<CampusResponse.VacationPeriod>> vacations =
                vacationRepository.findAllByOrderByStartDateAsc().stream()
                        .collect(Collectors.groupingBy(
                                UniversityVacation::getUniversityId,
                                Collectors.mapping(
                                        v -> new CampusResponse.VacationPeriod(
                                                v.getStartDate(), v.getEndDate()),
                                        Collectors.toList()
                                )
                        ));

        return universityRepository.findAll().stream()
                .filter(u -> allowed.contains(u.getId()))
                .map(u -> new CampusResponse(
                        u.getId(),
                        u.getName(),
                        u.getCampusName(),
                        u.getAddress(),
                        u.getLatitude(),
                        u.getLongitude(),
                        vacations.getOrDefault(u.getId(), List.of())
                ))
                .toList();
    }

    public List<CampusPlaceResponse> getPlaces(Long campusId) {
        if (!allowedIds().contains(campusId)
                || !universityRepository.existsById(campusId)) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND, "E404", "존재하지 않는 캠퍼스입니다");
        }

        return placeRepository.findByUniversityIdOrderByIdAsc(campusId).stream()
                .map(p -> new CampusPlaceResponse(
                        p.getId(),
                        p.getName(),
                        p.getDescription(),
                        p.getLatitude(),
                        p.getLongitude()
                ))
                .toList();
    }

    private Set<Long> allowedIds() {
        return Arrays.stream(allowedUniversityIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toSet());
    }
}
