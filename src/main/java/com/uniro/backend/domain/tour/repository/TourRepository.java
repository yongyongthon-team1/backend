package com.uniro.backend.domain.tour.repository;

import com.uniro.backend.domain.tour.entity.Tour;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TourRepository
        extends JpaRepository<Tour, Long>,
        JpaSpecificationExecutor<Tour> {
}