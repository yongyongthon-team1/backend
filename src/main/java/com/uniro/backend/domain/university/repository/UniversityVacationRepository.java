package com.uniro.backend.domain.university.repository;

import com.uniro.backend.domain.university.entity.UniversityVacation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UniversityVacationRepository
        extends JpaRepository<UniversityVacation, Long> {

    List<UniversityVacation> findAllByOrderByStartDateAsc();
}
