package com.uniro.backend.domain.store.repository;

import com.uniro.backend.domain.store.entity.StorePartnership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StorePartnershipRepository
        extends JpaRepository<StorePartnership, Long> {

    Optional<StorePartnership> findByStoreId(Long storeId);

    // 그날 제휴 기간 안에 있는 제휴 행사
    @Query("""
            select p from StorePartnership p
            where p.storeId in :storeIds
              and p.startDate <= :date
              and p.endDate >= :date
            """)
    List<StorePartnership> findActiveOn(
            @Param("storeIds") Collection<Long> storeIds,
            @Param("date") LocalDate date
    );

    // 오늘이 제휴 기간 안에 있는 상점 ID들
    @Query("""
            select p.storeId from StorePartnership p
            where p.storeId in :storeIds
              and p.startDate <= :today
              and p.endDate >= :today
            """)
    List<Long> findActiveStoreIds(
            @Param("storeIds") Collection<Long> storeIds,
            @Param("today") LocalDate today
    );
}
