package com.uniro.backend.domain.course.service;

import com.uniro.backend.common.ApiException;
import org.springframework.http.HttpStatus;

// 코스 조회·수정·확정·초안 생성이 함께 쓰는 오류 응답
final class CourseErrors {

    private CourseErrors() {
    }

    static ApiException tourNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "E404", "존재하지 않는 탐방입니다");
    }

    // 그 탐방의 가이드가 아니다
    static ApiException notGuide(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "E403", message);
    }

    // 취소되었거나 끝난 탐방이다
    static ApiException tourNotOpen() {
        return new ApiException(
                HttpStatus.CONFLICT, "E409_INVALID_TOUR_STATUS", "종료되었거나 취소된 탐방입니다");
    }

    static ApiException courseConfirmed() {
        return new ApiException(
                HttpStatus.CONFLICT, "E409_COURSE_CONFIRMED", "이미 확정된 코스입니다");
    }
}
