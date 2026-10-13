package com.guardian.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guardian.backend.dto.AnalyzeSnippetRequest;
import com.guardian.backend.dto.AuthResponse;
import com.guardian.backend.dto.CreateScanRequest;
import com.guardian.backend.dto.RegisterRequest;
import com.guardian.backend.entity.RepositoryScan;
import com.guardian.backend.entity.ScanStatus;
import com.guardian.backend.exception.MlServiceUnavailableException;
import com.guardian.backend.repository.RepositoryScanRepository;
import com.guardian.backend.service.MlServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class MlFailureTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RepositoryScanRepository scanRepository;

    @MockBean
    private MlServiceClient mlServiceClient;

    @Test
    void testMlFailureHandling() throws Exception {
        when(mlServiceClient.predictBugRisk(anyString(), anyString(), anyString()))
                .thenThrow(new MlServiceUnavailableException("ML service is currently unavailable"));

        // Register user
        RegisterRequest register = new RegisterRequest("mlfailuser", "mlfail@example.com", "Password123");
        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn();
        String token = objectMapper.readValue(regResult.getResponse().getContentAsString(), AuthResponse.class).getToken();

        // 1. /scans/analyze returns 503 and body does not contain "localhost"
        AnalyzeSnippetRequest analyzeRequest = new AnalyzeSnippetRequest("int a = 10;", "Repo", "App.java", "main");
        MvcResult analyzeResult = mockMvc.perform(post("/api/v1/scans/analyze")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(analyzeRequest)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andReturn();

        String responseBody = analyzeResult.getResponse().getContentAsString();
        assertFalse(responseBody.toLowerCase().contains("localhost"), "Error response must not leak localhost: " + responseBody);

        // 2. Batch scan ends FAILED
        CreateScanRequest batchScan = new CreateScanRequest("BatchRepo", "http://git/batch", "main", "c123");
        batchScan.setSnippets(List.of(
                new CreateScanRequest.SnippetItem("App.java", "test", "int a = 1;")
        ));

        mockMvc.perform(post("/api/v1/scans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchScan)))
                .andExpect(status().isServiceUnavailable());

        // Verify the created scan in repository ended with status FAILED
        List<RepositoryScan> userScans = scanRepository.findAllByOrderByCreatedAtDesc();
        assertFalse(userScans.isEmpty());
        RepositoryScan failedScan = userScans.stream()
                .filter(s -> "BatchRepo".equals(s.getRepoName()))
                .findFirst()
                .orElse(null);

        assertNotNull(failedScan);
        assertEquals(ScanStatus.FAILED, failedScan.getStatus());
    }
}
