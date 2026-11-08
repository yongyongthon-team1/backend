package com.uniro.backend.domain.store.support;

import com.uniro.backend.common.ApiException;
import org.springframework.http.HttpStatus;

// 상점 도메인에서 쓰는 오류 생성 헬퍼
public final class StoreErrors {

    private StoreErrors() {
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
