package com.guardian.backend.controller;

import com.guardian.backend.service.MlServiceClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final MlServiceClient mlServiceClient;

    public HealthController(MlServiceClient mlServiceClient) {
        this.mlServiceClient = mlServiceClient;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "GUARDIAN Spring Boot Backend");
        health.put("timestamp", LocalDateTime.now());

        Map<String, Object> mlHealth = mlServiceClient.checkHealth();
        health.put("mlService", mlHealth);

        return ResponseEntity.ok(health);
    }
}
