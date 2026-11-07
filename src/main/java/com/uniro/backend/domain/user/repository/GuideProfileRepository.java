package com.uniro.backend.domain.user.repository;

import com.uniro.backend.domain.user.entity.GuideProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuideProfileRepository
        extends JpaRepository<GuideProfile, Long> {
}