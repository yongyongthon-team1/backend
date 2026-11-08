package com.uniro.backend.domain.coupon.entity;

import com.uniro.backend.common.Kst;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

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

    // 쿠폰 코드 자리수. 컬럼 길이와 입력 검증이 같은 값을 쓴다.
    public static final int CODE_LENGTH = 6;

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
    @Column(name = "code", nullable = false, length = CODE_LENGTH, columnDefinition = "CHAR(6)")
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

    // 쿠폰 발급 (코스 확정 때). 만료는 탐방 당일 23:59:59.
    public Coupon(
            Long applicationId, Long storeId, Long partnershipId, String code,
            String discountSnapshot, LocalDateTime expiresAt
    ) {
        this.applicationId = applicationId;
        this.storeId = storeId;
        this.partnershipId = partnershipId;
        this.code = code;
        this.status = CouponStatus.ISSUED;
        this.discountSnapshot = discountSnapshot;
        this.issuedAt = Kst.nowToSeconds();
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() {
        if (this.issuedAt == null) {
            this.issuedAt = LocalDateTime.now();
        }
    }

    // 쿠폰의 날짜·시각은 모두 이 시간대 기준이다
    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    // 탐방 날짜 (쿠폰은 탐방 당일에만 쓸 수 있다)
    public LocalDate tourDate() {
        return expiresAt.toLocalDate();
    }

    // 유효기간 끝 (API 응답용, +09:00 포함)
    public OffsetDateTime validUntil() {
        return expiresAt.atZone(ZONE).toOffsetDateTime();
    }

    // 사용 시각 (API 응답용, +09:00 포함). 아직 사용하지 않았으면 null.
    public OffsetDateTime usedAtOffset() {
        return usedAt == null ? null : usedAt.atZone(ZONE).toOffsetDateTime();
    }

    // 지금 시각 기준의 상태. 탐방 날짜가 지난 발급 쿠폰은 EXPIRED로 본다.
    // 초 단위가 아니라 날짜로 비교해서, 탐방 당일 마지막 1초도 유효하다.
    public CouponStatus statusAt(LocalDateTime now) {
        if (status == CouponStatus.ISSUED && now.toLocalDate().isAfter(tourDate())) {
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
