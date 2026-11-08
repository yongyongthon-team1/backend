package com.uniro.backend.domain.store.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "stores")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 연결한 사장님 (NULL이면 아직 아무도 연결하지 않은 상점, 사장님 1명당 가게 1개)
    @Column(name = "owner_id", unique = true)
    private Long ownerId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    // 식당, 카페, 놀거리 등 (enum이 아니라 문자열)
    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @Column(name = "administrative_dong", length = 100)
    private String administrativeDong;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 사장님이 목록에 없는 가게를 직접 입력해 등록할 때 사용
    public Store(
            Long ownerId,
            String name,
            String address,
            String category,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        this.ownerId = ownerId;
        this.name = name;
        this.address = address;
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
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
