package com.uniro.backend.domain.store.repository;

import com.uniro.backend.domain.store.entity.Store;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByOwnerId(Long ownerId);

    boolean existsByOwnerId(Long ownerId);

    boolean existsByNameAndAddress(String name, String address);

    boolean existsByNameAndAddressAndIdNot(String name, String address, Long id);

    // 같은 상점을 두 사장님이 동시에 연결하지 못하도록 행을 잠그고 조회한다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Store s where s.id = :id")
    Optional<Store> findByIdForUpdate(@Param("id") Long id);

    // 상호 부분 일치 검색 (%, _ 는 Spring Data가 이스케이프하고,
    // MySQL 기본 collation이 대소문자를 구분하지 않으므로 IgnoreCase는 쓰지 않는다)
    Page<Store> findByNameContaining(String keyword, Pageable pageable);
}
