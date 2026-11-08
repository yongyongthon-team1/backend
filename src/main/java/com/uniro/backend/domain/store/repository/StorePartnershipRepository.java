package com.uniro.backend.domain.store.repository;

import com.uniro.backend.domain.store.entity.StorePartnership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StorePartnershipRepository
        extends JpaRepository<StorePartnership, Long> {

    Optional<StorePartnership> findByStoreId(Long storeId);
}
