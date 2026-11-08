package com.uniro.backend.domain.course.service;

import com.uniro.backend.common.ApiException;
import com.uniro.backend.domain.course.dto.CourseResponse;
import com.uniro.backend.domain.course.entity.Course;
import com.uniro.backend.domain.course.repository.CourseRepository;
import com.uniro.backend.domain.course.repository.CourseStopRepository;
import com.uniro.backend.domain.tour.entity.Tour;
import com.uniro.backend.domain.tour.entity.TourApplicationStatus;
import com.uniro.backend.domain.tour.repository.TourApplicationRepository;
import com.uniro.backend.domain.tour.repository.TourRepository;
import com.uniro.backend.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 탐방 코스 조회·수정·확정
@Service
@RequiredArgsConstructor
public class CourseService {

    private final TourRepository tourRepository;
    private final TourApplicationRepository applicationRepository;
    private final CourseRepository courseRepository;
    private final CourseStopRepository stopRepository;
    private final CourseAssembler assembler;

    // 코스 조회. 가이드는 초안도 볼 수 있고, 승인된 신청자는 확정된 코스만 볼 수 있다.
    @Transactional(readOnly = true)
    public CourseResponse view(User user, Long tourId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "E404", "존재하지 않는 탐방입니다"));

        boolean guide = tour.getGuideId().equals(user.getId());
        if (!guide && !applicationRepository.existsByTourIdAndUserIdAndStatusIn(
                tourId, user.getId(), List.of(TourApplicationStatus.APPROVED))) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN, "E403", "이 탐방의 코스를 볼 수 없어요");
        }

        Course course = courseRepository.findById(tourId).orElse(null);

        // 신청자에게는 없는 코스와 확정 전 코스를 구분하지 않고 준비 중이라고 알린다
        if (!guide && (course == null || !course.isConfirmed())) {
            throw notReady();
        }
        if (course == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND, "E404", "아직 만들어진 코스가 없습니다");
        }

        return assembler.assemble(tour, course, stopRepository.findByTourIdOrderByStopOrderAsc(tourId));
    }

    private ApiException notReady() {
        return new ApiException(
                HttpStatus.CONFLICT, "E409_COURSE_NOT_READY", "가이드가 코스를 준비 중이에요");
    }
}
