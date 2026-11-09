package com.uniro.backend.common.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

// Google Gemini API 호출. 키는 요청 헤더로만 보내고, 프롬프트와 키는 로그에 남기지 않는다.
@Slf4j
@Component
public class GeminiAiClient implements AiClient {

    // 서버 오류나 연결 문제일 때 한 번 더 시도한다
    private static final int MAX_ATTEMPTS = 2;
    private static final int MAX_OUTPUT_TOKENS = 2048;
    private static final double TEMPERATURE = 0.4;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final Duration timeout;

    public GeminiAiClient(
            ObjectMapper objectMapper,
            @Value("${ai.gemini.api-key:}") String apiKey,
            @Value("${ai.gemini.model}") String model,
            @Value("${ai.gemini.base-url}") String baseUrl,
            @Value("${ai.gemini.timeout-seconds:15}") int timeoutSeconds
    ) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.baseUrl = baseUrl;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    @Override
    public boolean enabled() {
        return !apiKey.isEmpty();
    }

    @Override
    public JsonNode generateJson(String prompt) {
        if (!enabled()) {
            throw new AiException("AI 키가 설정되지 않았습니다");
        }

        String body = requestBody(prompt);
        AiException last = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return parse(send(body));
            } catch (RetryableException e) {
                last = new AiException(e.getMessage(), e);
                log.warn("Gemini 호출 실패({}/{}): {}", attempt, MAX_ATTEMPTS, e.getMessage());
            }
        }

        throw last;
    }

    private String requestBody(String prompt) {
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "maxOutputTokens", MAX_OUTPUT_TOKENS,
                        "temperature", TEMPERATURE
                )
        );

        return objectMapper.writeValueAsString(body);
    }

    // 성공하면 응답 본문을, 다시 시도해 볼 만한 실패면 RetryableException을 던진다
    private String send(String body) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/models/" + model + ":generateContent"))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiException("AI 호출이 중단되었습니다", e);
        } catch (Exception e) {
            // 시간 초과, 연결 실패
            throw new RetryableException("AI 서버에 연결하지 못했습니다: " + e.getClass().getSimpleName());
        }

        int status = response.statusCode();
        if (status >= 500) {
            throw new RetryableException("AI 서버 오류: HTTP " + status);
        }
        if (status == 429) {
            // 무료 한도 초과. 키 오류와 구별되게 남긴다.
            throw new AiException("AI 호출 한도를 넘었습니다(HTTP 429)");
        }
        if (status != 200) {
            // 키 오류, 한도 초과, 잘못된 요청은 다시 보내도 같으므로 바로 실패시킨다
            throw new AiException("AI 호출 실패: HTTP " + status);
        }

        return response.body();
    }

    private JsonNode parse(String responseBody) {
        try {
            String text = objectMapper.readTree(responseBody)
                    .path("candidates").path(0).path("content").path("parts").path(0)
                    .path("text").asString("");

            if (text.isBlank()) {
                throw new AiException("AI가 빈 응답을 돌려주었습니다");
            }

            return objectMapper.readTree(stripCodeFence(text));
        } catch (AiException e) {
            throw e;
        } catch (Exception e) {
            throw new AiException("AI 응답을 읽을 수 없습니다", e);
        }
    }

    // JSON으로 답하라고 해도 ```json 으로 감싸 오는 경우를 벗겨 준다
    private String stripCodeFence(String text) {
        String trimmed = text.trim();

        if (!trimmed.startsWith("```")) {
            return trimmed;
        }

        int firstLineEnd = trimmed.indexOf('\n');
        int lastFence = trimmed.lastIndexOf("```");
        if (firstLineEnd < 0 || lastFence <= firstLineEnd) {
            return trimmed;
        }

        return trimmed.substring(firstLineEnd + 1, lastFence).trim();
    }

    private static class RetryableException extends RuntimeException {
        RetryableException(String message) {
            super(message);
        }
    }
}
