package com.uniro.backend.domain.tour.repository;

import com.uniro.backend.domain.tour.entity.TourApplication;
import com.uniro.backend.domain.tour.entity.TourApplicationStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    // 신청의 주인(신청자)과 탐방·가이드. 탐방 행을 먼저 잠그고 권한은 잠그기 전에 확인하기 위해,
    // 신청 행 전체가 아니라 바뀌지 않는 값만 잠금 없이 읽는다.
    @Query("select new com.uniro.backend.domain.tour.repository.TourApplicationRepository$ApplicationRef("
            + "a.tourId, a.userId, t.guideId) "
            + "from TourApplication a join Tour t on t.id = a.tourId where a.id = :id")
    Optional<ApplicationRef> findRefById(@Param("id") Long id);

    record ApplicationRef(Long tourId, Long applicantId, Long guideId) {
    }

    // 탐방의 신청자 목록 (신청 시각이 빠른 순). status가 null이면 전체.
    // 학년은 프로필이 없으면 null이다.
    @Query("select new com.uniro.backend.domain.tour.repository.TourApplicationRepository$ApplicantRow("
            + "a.id, u.name, p.grade, a.motivation, a.status, a.createdAt) "
            + "from TourApplication a "
            + "join User u on u.id = a.userId "
            + "left join StudentProfile p on p.id = a.userId "
            + "where a.tourId = :tourId and (:status is null or a.status = :status) "
            + "order by a.createdAt asc, a.id asc")
    List<ApplicantRow> findApplicants(
            @Param("tourId") Long tourId,
            @Param("status") TourApplicationStatus status
    );

    record ApplicantRow(
            Long applicationId, String name, Integer grade, String wish,
            TourApplicationStatus status, LocalDateTime createdAt
    ) {
    }

    // 탐방의 해당 상태 신청 ID들
    @Query("select a.id from TourApplication a where a.tourId = :tourId and a.status = :status")
    List<Long> findIdsByTourIdAndStatus(
            @Param("tourId") Long tourId, @Param("status") TourApplicationStatus status);

    // 탐방의 해당 상태 신청자들이 쓴 "하고 싶은 것" (신청 순)
    @Query("select a.motivation from TourApplication a "
            + "where a.tourId = :tourId and a.status = :status order by a.createdAt, a.id")
    List<String> findMotivationsByTourIdAndStatus(
            @Param("tourId") Long tourId, @Param("status") TourApplicationStatus status);

    // 이미 유효한(신청 중·승인) 신청이 있는지
    boolean existsByTourIdAndUserIdAndStatusIn(
            Long tourId,
            Long userId,
            Collection<TourApplicationStatus> statuses
    );
}