package com.uniro.backend.domain.verification.client;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class NtsBusinessClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String serviceKey;

    public NtsBusinessClient(
            ObjectMapper objectMapper,
            @Value("${nts.business.base-url}") String baseUrl,
            @Value("${nts.business.service-key}") String serviceKey
    ) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.serviceKey = serviceKey;
    }

    public boolean isMatching(
            String businessNumber,
            String representativeName,
            String openedAt
    ) {
        Map<String, Object> body = Map.of(
                "businesses", List.of(Map.of(
                        "b_no", businessNumber,
                        "start_dt", openedAt,
                        "p_nm", representativeName
                ))
        );

        JsonNode response = post("/validate", body);
        JsonNode data = firstData(response);

        return "01".equals(data.path("valid").asText());
    }

    public boolean isActive(String businessNumber) {
        Map<String, Object> body = Map.of(
                "b_no", List.of(businessNumber)
        );

        JsonNode response = post("/status", body);
        JsonNode data = firstData(response);

        return "01".equals(data.path("b_stt_cd").asText());
    }

    private JsonNode firstData(JsonNode response) {
        JsonNode data = response.path("data");

        if (!data.isArray() || data.isEmpty()) {
            throw new NtsApiException("국세청 API 응답에 조회 결과가 없습니다");
        }

        return data.get(0);
    }

    private JsonNode post(String path, Object body) {
        try {
            String encodedKey = URLEncoder.encode(
                    serviceKey,
                    StandardCharsets.UTF_8
            );

            String url = baseUrl + path
                    + "?serviceKey=" + encodedKey;

            String json = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() != 200) {
                throw new NtsApiException(
                        "국세청 API 호출 실패: HTTP "
                                + response.statusCode()
                );
            }

            JsonNode result = objectMapper.readTree(response.body());

            if (!"OK".equals(result.path("status_code").asText())) {
                throw new NtsApiException(
                        "국세청 API가 정상 응답을 반환하지 않았습니다"
                );
            }

            return result;

        } catch (NtsApiException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NtsApiException("국세청 API 호출이 중단되었습니다", e);
        } catch (Exception e) {
            throw new NtsApiException("국세청 API 연결에 실패했습니다", e);
        }
    }
}