package com.uniro.backend.domain.tourwish.repository;

import com.uniro.backend.domain.tourwish.entity.TourWish;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TourWishRepository
        extends JpaRepository<TourWish, Long>,
        JpaSpecificationExecutor<TourWish> {

    Page<TourWish> findByUserId(
            Long userId,
            Pageable pageable
    );
}