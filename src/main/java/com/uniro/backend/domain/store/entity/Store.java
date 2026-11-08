package com.uniro.backend.domain.store.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "stores",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_stores_name_address",
                columnNames = {"name", "address"}
        ),
        // 주변 상점 조회의 좌표 범위 검색용
        indexes = @Index(
                name = "idx_stores_lat_lng",
                columnList = "latitude, longitude"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store {

    // 입력 검증과 컬럼 길이가 어긋나지 않도록 한 곳에서 정한다
    public static final int NAME_MAX = 150;
    public static final int ADDRESS_MAX = 255;
    public static final int CATEGORY_MAX = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 연결한 사장님 (NULL이면 아직 아무도 연결하지 않은 상점, 사장님 1명당 가게 1개)
    @Column(name = "owner_id", unique = true)
    private Long ownerId;

    @Column(name = "name", nullable = false, length = NAME_MAX)
    private String name;

    @Column(name = "address", nullable = false, length = ADDRESS_MAX)
    private String address;

    // 식당, 카페, 놀거리 등 (enum이 아니라 문자열)
    @Column(name = "category", nullable = false, length = CATEGORY_MAX)
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

    // 사장님 계정에 연결
    public void assignOwner(Long ownerId) {
        this.ownerId = ownerId;
    }

    // 보낸 항목만 수정 (null이면 그대로 둔다)
    public void update(String name, String address, String category) {
        if (name != null) {
            this.name = name;
        }
        if (address != null) {
            this.address = address;
        }
        if (category != null) {
            this.category = category;
        }
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
