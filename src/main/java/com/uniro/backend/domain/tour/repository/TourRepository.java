package com.uniro.backend.domain.tour.repository;

import com.uniro.backend.domain.tour.entity.Tour;

import com.uniro.backend.domain.tour.entity.TourStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TourRepository
        extends JpaRepository<Tour, Long>,
        JpaSpecificationExecutor<Tour> {
    Page<Tour> findByGuideId(
            Long guideId,
            Pageable pageable
    );

    Page<Tour> findByGuideIdAndStatus(
            Long guideId,
            TourStatus status,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Tour t where t.id = :id")
    Optional<Tour> findByIdForUpdate(@Param("id") Long id);
}
