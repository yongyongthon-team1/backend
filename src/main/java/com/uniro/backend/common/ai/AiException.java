package com.uniro.backend.common.ai;

// AI 호출이 실패했다(키 없음, 시간 초과, 오류 응답, 읽을 수 없는 응답).
// 이 예외를 어떤 HTTP 응답으로 바꿀지는 AI를 부르는 기능이 정한다.
public class AiException extends RuntimeException {

    public AiException(String message) {
        super(message);
    }

    public AiException(String message, Throwable cause) {
        super(message, cause);
    }
}
