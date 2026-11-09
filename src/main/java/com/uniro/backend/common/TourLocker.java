package com.uniro.backend.common;

import com.uniro.backend.domain.tour.entity.Tour;
import com.uniro.backend.domain.tour.repository.TourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

// 탐방 행을 잠그고 읽는다. 신청 승인·취소, 코스 수정·확정처럼 탐방 하나를 두고 겹치면 안 되는
// 작업이 사용한다. 잠금은 그 트랜잭션의 첫 DB 작업이어야 한다(그 전의 조회가 읽기 기준을 정해 버린다).
@Component
@RequiredArgsConstructor
public class TourLocker {

    private final TourRepository tourRepository;

    public Tour lock(Long tourId) {
        try {
            return tourRepository.findByIdForUpdate(tourId)
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND, "E404", "존재하지 않는 탐방입니다"));
        } catch (PessimisticLockingFailureException e) {
            // 동시 요청과 겹쳐 잠금 대기 초과나 데드락으로 끊긴 경우
            throw new ApiException(
                    HttpStatus.CONFLICT, "E409", "잠시 후 다시 시도해 주세요");
        }
    }
}
