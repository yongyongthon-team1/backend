package com.uniro.backend.common.ai;

// AI 프롬프트에 사용자가 쓴 글을 넣을 때 쓰는 정리 도우미
public final class PromptText {

    private PromptText() {
    }

    // 글이 프롬프트의 <리뷰>·<희망>·<후보> 구간을 닫는 태그나 줄바꿈으로 프롬프트 구조를 벗어나지 못하게 한다
    public static String plain(String text) {
        return text.replace('<', ' ').replace('>', ' ').replaceAll("\\s+", " ").trim();
    }
}
