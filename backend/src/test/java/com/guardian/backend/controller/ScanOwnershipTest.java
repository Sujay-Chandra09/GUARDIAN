package com.guardian.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guardian.backend.dto.*;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ScanOwnershipTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MlServiceClient mlServiceClient;

    @Test
    void userBShouldNotAccessUserAScanAndAdminShouldAccessIt() throws Exception {
        // Mock ML service
        MlPredictResponse mlResponse = new MlPredictResponse();
        mlResponse.setRiskScore(0.5);
        mlResponse.setBuggy(false);
        mlResponse.setRiskLevel("MEDIUM");
        mlResponse.setLatencyMs(30);
        mlResponse.setExplanation("All good");
        when(mlServiceClient.predictBugRisk(anyString(), anyString(), anyString())).thenReturn(mlResponse);

        // 1. Register User A
        RegisterRequest userARequest = new RegisterRequest("usera", "usera@example.com", "Password123");
        MvcResult userAResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userARequest)))
                .andExpect(status().isCreated())
                .andReturn();
        String tokenA = objectMapper.readValue(userAResult.getResponse().getContentAsString(), AuthResponse.class).getToken();

        // User A creates a scan
        CreateScanRequest createScanRequest = new CreateScanRequest("UserARepo", "http://github.com/usera/repo", "main", "c1");
        MvcResult scanResult = mockMvc.perform(post("/api/v1/scans")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createScanRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        Long scanId = objectMapper.readValue(scanResult.getResponse().getContentAsString(), ScanResponseDto.class).getId();

        // 2. Register User B
        RegisterRequest userBRequest = new RegisterRequest("userb", "userb@example.com", "Password123");
        MvcResult userBResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userBRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        String tokenB = objectMapper.readValue(userBResult.getResponse().getContentAsString(), AuthResponse.class).getToken();

        // User B tries GET /scans/{id} -> 404
        mockMvc.perform(get("/api/v1/scans/" + scanId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // User B tries GET /scans/{id}/summary -> 404
        mockMvc.perform(get("/api/v1/scans/" + scanId + "/summary")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // User B tries appending via repositoryScanId -> 404
        AnalyzeSnippetRequest appendRequest = new AnalyzeSnippetRequest("int x = 1;", "UserARepo", "A.java", "fn");
        appendRequest.setRepositoryScanId(scanId);
        mockMvc.perform(post("/api/v1/scans/analyze")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appendRequest)))
                .andExpect(status().isNotFound());

        // 3. Admin logs in
        AuthRequest adminLogin = new AuthRequest("admin", "Admin@12345");
        MvcResult adminResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();
        String adminToken = objectMapper.readValue(adminResult.getResponse().getContentAsString(), AuthResponse.class).getToken();

        // Admin can read user A's scan and summary -> 200
        mockMvc.perform(get("/api/v1/scans/" + scanId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/scans/" + scanId + "/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
