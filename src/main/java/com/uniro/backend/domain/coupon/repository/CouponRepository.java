package com.uniro.backend.domain.coupon.repository;

import com.uniro.backend.domain.coupon.entity.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);

    // 잠금 없이 쿠폰이 어느 가게 것인지만 확인한다 (엔티티를 읽어 두지 않아 이후 잠금 조회가 최신 상태를 읽는다)
    @Query("select c.storeId from Coupon c where c.code = :code")
    Optional<Long> findStoreIdByCode(@Param("code") String code);

    // 같은 쿠폰이 동시에 사용 처리되지 않도록 행을 잠그고 조회한다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Coupon c where c.code = :code")
    Optional<Coupon> findByCodeForUpdate(@Param("code") String code);

    // 내 신청으로 발급된 쿠폰 (최신 발급순). tourId, storeId가 null이면 그 조건은 쓰지 않는다.
    @Query("""
            select new com.uniro.backend.domain.coupon.repository.MyCouponRow(
                c,
                a.tourId,
                case when a.status = com.uniro.backend.domain.tour.entity.TourApplicationStatus.APPROVED
                     then true else false end,
                s.name
            )
            from Coupon c
            join TourApplication a on a.id = c.applicationId
            left join Store s on s.id = c.storeId
            where a.userId = :userId
              and (:tourId is null or a.tourId = :tourId)
              and (:storeId is null or c.storeId = :storeId)
            order by c.id desc
            """)
    List<MyCouponRow> findMine(
            @Param("userId") Long userId,
            @Param("tourId") Long tourId,
            @Param("storeId") Long storeId
    );

    // 그 가게에서 이날 이미 사용 처리된 쿠폰이 있는지
    boolean existsByStoreIdAndUsedDate(Long storeId, LocalDate usedDate);
}
