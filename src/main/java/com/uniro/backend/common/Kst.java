package com.uniro.backend.common;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

// 신청 도메인의 시간 처리: DB의 LocalDateTime은 한국 시간(Asia/Seoul) 기준이다.
public final class Kst {

    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private Kst() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    // DB 시각 정밀도(마이크로초)와 응답이 어긋나지 않도록 초 단위로 맞춘 지금 시각
    public static LocalDateTime nowToSeconds() {
        return now().truncatedTo(ChronoUnit.SECONDS);
    }

    // 응답용 +09:00 시각
    public static OffsetDateTime toOffset(LocalDateTime time) {
        return time.atZone(ZONE).toOffsetDateTime();
    }
}
