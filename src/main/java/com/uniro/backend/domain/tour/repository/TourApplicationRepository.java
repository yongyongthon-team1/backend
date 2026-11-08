package com.uniro.backend.domain.tour.repository;

import com.uniro.backend.domain.tour.entity.TourApplication;
import com.uniro.backend.domain.tour.entity.TourApplicationStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface TourApplicationRepository
        extends JpaRepository<TourApplication, Long> {

    // 기존: 유효 신청 수 계산
    long countByTourIdAndStatusIn(
            Long tourId,
            Collection<TourApplicationStatus> statuses
    );

    // 신규: 특정 사용자의 해당 탐방 신청 내역 조회
    Optional<TourApplication>
    findTopByTourIdAndUserIdOrderByCreatedAtDescIdDesc(
            Long tourId,
            Long userId
    );
}