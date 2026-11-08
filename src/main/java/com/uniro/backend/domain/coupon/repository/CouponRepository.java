package com.uniro.backend.domain.coupon.repository;

import com.uniro.backend.domain.coupon.entity.Coupon;
import com.uniro.backend.domain.coupon.entity.CouponStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
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

    // 주어진 신청들의 아직 쓰지 않은(발급 상태) 쿠폰을 만료로 바꾼다. 이미 사용된 쿠폰은 그대로 둔다.
    // 호출한 쪽이 이미 읽어 둔 엔티티를 분리시키지 않도록 영속성 컨텍스트는 비우지 않는다.
    @Modifying(flushAutomatically = true)
    @Query("""
            update Coupon c
            set c.status = com.uniro.backend.domain.coupon.entity.CouponStatus.EXPIRED
            where c.applicationId in :applicationIds
              and c.status = com.uniro.backend.domain.coupon.entity.CouponStatus.ISSUED
            """)
    int expireIssuedByApplicationIds(
            @Param("applicationIds") Collection<Long> applicationIds);

    // 그 가게에서 이날 이미 사용 처리된 쿠폰이 있는지
    boolean existsByStoreIdAndUsedDate(Long storeId, LocalDate usedDate);

    // 리뷰를 쓸 수 있는지 확인하는 데 필요한 값: 쿠폰의 상점·상태와, 그 신청의 신청자·탐방
    @Query("""
            select new com.uniro.backend.domain.coupon.repository.CouponRepository$ReviewTarget(
                c.storeId, c.status, a.userId, a.tourId)
            from Coupon c
            join TourApplication a on a.id = c.applicationId
            where c.id = :id
            """)
    Optional<ReviewTarget> findReviewTarget(@Param("id") Long id);

    record ReviewTarget(Long storeId, CouponStatus status, Long userId, Long tourId) {
    }
}
