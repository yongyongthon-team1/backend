package com.uniro.backend.common.ai;

import tools.jackson.databind.JsonNode;

// 생성형 AI 호출. 제공자(현재 Gemini)에 기능 코드가 묶이지 않도록 이 인터페이스만 쓴다.
public interface AiClient {

    // 키가 설정돼 있어 실제로 호출할 수 있는지. 아니면 generateJson이 AiException으로 실패하므로,
    // 호출 전에 미리 확인해 헛된 작업을 건너뛰거나(리뷰 요약) 바로 실패시킨다(코스 초안).
    boolean enabled();

    // 프롬프트를 보내고 JSON으로 답한 내용을 돌려준다. 실패하면 AiException.
    // 개인정보가 들어가지 않는 내용만 보내야 한다(이 클라이언트는 프롬프트를 로그에 남기지 않는다).
    JsonNode generateJson(String prompt);
}
