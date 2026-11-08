package com.uniro.backend.domain.university.repository;

import com.uniro.backend.domain.university.entity.CampusPlace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampusPlaceRepository
        extends JpaRepository<CampusPlace, Long> {

    List<CampusPlace> findByUniversityIdOrderByIdAsc(Long universityId);
}
