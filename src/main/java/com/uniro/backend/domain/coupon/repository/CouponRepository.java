package com.uniro.backend.domain.coupon.repository;

import com.uniro.backend.domain.coupon.entity.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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

    // 그 가게에서 이날 이미 사용 처리된 쿠폰이 있는지
    boolean existsByStoreIdAndUsedDate(Long storeId, LocalDate usedDate);
}
