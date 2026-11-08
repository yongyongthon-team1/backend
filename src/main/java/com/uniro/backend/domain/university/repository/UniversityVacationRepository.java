package com.uniro.backend.domain.university.repository;

import com.uniro.backend.domain.university.entity.UniversityVacation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface UniversityVacationRepository
        extends JpaRepository<UniversityVacation, Long> {

    // 기존 캠퍼스 조회 API에서 사용
    List<UniversityVacation> findAllByOrderByStartDateAsc();

    // 신규 탐방 개설 API에서 사용
    boolean existsByUniversityIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long universityId,
            LocalDate startDate,
            LocalDate endDate
    );
}