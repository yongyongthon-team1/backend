package com.uniro.backend.common;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

// 신청 도메인의 시간 처리: DB의 LocalDateTime은 한국 시간(Asia/Seoul) 기준이다.
public final class Kst {

    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private Kst() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    // 응답용 +09:00 시각
    public static OffsetDateTime toOffset(LocalDateTime time) {
        return time.atZone(ZONE).toOffsetDateTime();
    }
}
