package com.guardian.backend.service;

import com.guardian.backend.dto.MlPredictRequest;
import com.guardian.backend.dto.MlPredictResponse;
import com.guardian.backend.exception.MlServiceException;
import com.guardian.backend.exception.MlServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Service
public class MlServiceClient {

    private static final Logger log = LoggerFactory.getLogger(MlServiceClient.class);

    private final WebClient webClient;

    @Value("${guardian.ml-service.timeout-seconds:10}")
    private long timeoutSeconds;

    public MlServiceClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public MlPredictResponse predictBugRisk(String codeSnippet, String repo, String filepath) {
        MlPredictRequest request = new MlPredictRequest(codeSnippet, repo, filepath);

        try {
            MlPredictResponse response = webClient.post()
                    .uri("/predict")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .defaultIfEmpty("Invalid ML request parameters")
                                    .flatMap(errorBody -> {
                                        log.error("ML service returned 4xx client error: {}", errorBody);
                                        return Mono.error(new IllegalArgumentException("ML service rejected request: " + errorBody));
                                    })
                    )
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .defaultIfEmpty("ML internal server error")
                                    .flatMap(errorBody -> {
                                        log.error("ML service returned 5xx server error: {}", errorBody);
                                        return Mono.error(new MlServiceException("ML service encountered an internal error"));
                                    })
                    )
                    .bodyToMono(MlPredictResponse.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            if (response == null) {
                log.error("ML service returned null body for prediction");
                throw new MlServiceUnavailableException("ML service returned an empty response");
            }

            return response;
        } catch (IllegalArgumentException | MlServiceException | MlServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to communicate with GUARDIAN ML service: {}", e.getMessage(), e);
            throw new MlServiceUnavailableException("ML service is currently unavailable", e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> checkHealth() {
        try {
            return webClient.get()
                    .uri("/health")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(3))
                    .onErrorReturn(Map.of("status", "unreachable", "error", "ML service offline"))
                    .block();
        } catch (Exception e) {
            log.error("ML service health check failed: {}", e.getMessage());
            return Map.of("status", "unreachable", "error", "ML service offline");
        }
    }
}
