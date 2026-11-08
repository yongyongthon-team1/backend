package com.uniro.backend.domain.coupon.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 제휴 쿠폰. 탐방 신청자 한 명이 코스 안의 제휴 상점마다 한 장씩 받는다.
@Entity
@Getter
@Table(
        name = "coupons",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_coupons_application_store",
                        columnNames = {"application_id", "store_id"}
                ),
                @UniqueConstraint(
                        name = "uk_coupons_code",
                        columnNames = "code"
                ),
                // 같은 날 같은 가게에서는 쿠폰 한 장만 사용 처리된다
                @UniqueConstraint(
                        name = "uk_coupons_store_used_date",
                        columnNames = {"store_id", "used_date"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "partnership_id", nullable = false)
    private Long partnershipId;

    // 6자리 쿠폰 코드
    @Column(name = "code", nullable = false, length = 6, columnDefinition = "CHAR(6)")
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CouponStatus status = CouponStatus.ISSUED;

    // 발급 당시 할인 내용 (이후 제휴 내용이 바뀌어도 쿠폰은 그대로)
    @Column(name = "discount_snapshot", nullable = false)
    private String discountSnapshot;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    // 탐방 당일 23:59:59
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    // 사용 처리한 날짜. (store_id, used_date) 유니크로 하루 한 장을 DB에서도 보장한다.
    @Column(name = "used_date")
    private LocalDate usedDate;

    @PrePersist
    void onCreate() {
        if (this.issuedAt == null) {
            this.issuedAt = LocalDateTime.now();
        }
    }

    // 탐방 날짜 (쿠폰은 탐방 당일에만 쓸 수 있다)
    public LocalDate tourDate() {
        return expiresAt.toLocalDate();
    }

    // 지금 시각 기준의 상태. 유효기간이 지난 발급 쿠폰은 EXPIRED로 본다.
    public CouponStatus statusAt(LocalDateTime now) {
        if (status == CouponStatus.ISSUED && now.isAfter(expiresAt)) {
            return CouponStatus.EXPIRED;
        }

        return status;
    }

    // 사용 처리
    public void use(LocalDateTime now) {
        this.status = CouponStatus.USED;
        this.usedAt = now;
        this.usedDate = now.toLocalDate();
    }
}
