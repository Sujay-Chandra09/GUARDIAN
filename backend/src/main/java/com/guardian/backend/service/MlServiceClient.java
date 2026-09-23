package com.guardian.backend.service;

import com.guardian.backend.dto.MlPredictRequest;
import com.guardian.backend.dto.MlPredictResponse;
import com.guardian.backend.exception.MlServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
            return webClient.post()
                    .uri("/predict")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(MlPredictResponse.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .onErrorResume(ex -> {
                        log.error("Failed to communicate with GUARDIAN ML service: {}", ex.getMessage());
                        return Mono.error(new MlServiceUnavailableException(
                                "GUARDIAN Python ML service is unreachable at port 8000: " + ex.getMessage(), ex
                        ));
                    })
                    .block();
        } catch (MlServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during ML prediction: {}", e.getMessage());
            throw new MlServiceUnavailableException("ML service prediction failed: " + e.getMessage(), e);
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
            return Map.of("status", "unreachable", "error", e.getMessage());
        }
    }
}
