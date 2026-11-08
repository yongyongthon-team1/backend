package com.uniro.backend.domain.store.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 상점 제휴 할인. 상점당 하나이고 다시 저장하면 같은 행을 덮어쓴다.
@Entity
@Getter
@Table(
        name = "store_partnerships",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_store_partnerships_store",
                columnNames = "store_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StorePartnership {

    // API에서 받는 할인 내용 최대 길이 (컬럼은 255자)
    public static final int DISCOUNT_MAX = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "discount_description", nullable = false, length = 255)
    private String discountDescription;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public StorePartnership(
            Long storeId,
            String discountDescription,
            LocalDate startDate,
            LocalDate endDate
    ) {
        this.storeId = storeId;
        this.discountDescription = discountDescription;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void update(
            String discountDescription,
            LocalDate startDate,
            LocalDate endDate
    ) {
        this.discountDescription = discountDescription;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
