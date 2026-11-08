package com.uniro.backend.domain.store.repository;

import com.uniro.backend.domain.store.entity.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByOwnerId(Long ownerId);

    boolean existsByOwnerId(Long ownerId);

    // 상호 부분 일치 검색 (%, _ 는 Spring Data가 이스케이프하고,
    // MySQL 기본 collation이 대소문자를 구분하지 않으므로 IgnoreCase는 쓰지 않는다)
    Page<Store> findByNameContaining(String keyword, Pageable pageable);
}
