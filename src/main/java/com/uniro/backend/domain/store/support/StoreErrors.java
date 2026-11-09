package com.uniro.backend.domain.store.support;

import com.uniro.backend.common.ApiException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

// 상점 도메인에서 쓰는 오류 생성 헬퍼
public final class StoreErrors {

    // 받을 수 있는 날짜의 범위. DB의 DATE/DATETIME 범위를 벗어나는 비정상 날짜를 막는다.
    private static final LocalDate MIN_DATE = LocalDate.of(2000, 1, 1);
    private static final LocalDate MAX_DATE = LocalDate.of(2100, 12, 31);

    private StoreErrors() {
    }

    // 비정상적으로 먼 날짜는 400으로 막는다
    public static void requireReasonableDate(String field, LocalDate value) {
        if (value.isBefore(MIN_DATE) || value.isAfter(MAX_DATE)) {
            throw invalid(
                    field, MIN_DATE.getYear() + "~" + MAX_DATE.getYear() + "년 사이여야 합니다");
        }
    }

    // 요청 값 오류(400): 어떤 필드가 왜 잘못됐는지 errors에 담긴다
    public static ApiException invalid(String field, String reason) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "E400",
                "요청 값이 유효하지 않습니다",
                field,
                reason
        );
    }
}
