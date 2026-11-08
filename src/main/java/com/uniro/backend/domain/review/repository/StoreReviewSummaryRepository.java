package com.uniro.backend.domain.review.repository;

import com.uniro.backend.domain.review.entity.StoreReviewSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreReviewSummaryRepository extends JpaRepository<StoreReviewSummary, Long> {

    Optional<StoreReviewSummary> findByStoreId(Long storeId);
}
