package com.uniro.backend.domain.tour.repository;

import com.uniro.backend.domain.tour.entity.TourApplication;
import com.uniro.backend.domain.tour.entity.TourApplicationStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
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

    Page<TourApplication> findByUserId(
            Long userId,
            Pageable pageable
    );

    long countByTourIdAndStatus(
            Long tourId,
            TourApplicationStatus status
    );

    List<TourApplication> findByTourId(Long tourId);

    // 신청이 속한 탐방 ID (탐방 행을 먼저 잠그기 위해 신청 행은 읽지 않고 ID만 본다)
    @Query("select a.tourId from TourApplication a where a.id = :id")
    Optional<Long> findTourIdById(@Param("id") Long id);

    // 이미 유효한(신청 중·승인) 신청이 있는지
    boolean existsByTourIdAndUserIdAndStatusIn(
            Long tourId,
            Long userId,
            Collection<TourApplicationStatus> statuses
    );
}