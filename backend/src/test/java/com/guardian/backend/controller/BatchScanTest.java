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

import java.util.List;

import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class BatchScanTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MlServiceClient mlServiceClient;

    @Test
    void testBatchScanAndIsBuggyField() throws Exception {
        // Arrange mock ML responses
        MlPredictResponse highBuggy = new MlPredictResponse();
        highBuggy.setRiskScore(0.85);
        highBuggy.setBuggy(true);
        highBuggy.setRiskLevel("HIGH");
        highBuggy.setLatencyMs(20);
        highBuggy.setExplanation("High severity bug");

        MlPredictResponse lowClean = new MlPredictResponse();
        lowClean.setRiskScore(0.20);
        lowClean.setBuggy(false);
        lowClean.setRiskLevel("LOW");
        lowClean.setLatencyMs(15);
        lowClean.setExplanation("Clean code");

        when(mlServiceClient.predictBugRisk(eq("snippet1"), eq("BatchRepo"), eq("File1.java")))
                .thenReturn(highBuggy);
        when(mlServiceClient.predictBugRisk(eq("snippet2"), eq("BatchRepo"), eq("File2.java")))
                .thenReturn(lowClean);
        when(mlServiceClient.predictBugRisk(eq("snippet-single"), eq("unknown"), eq("Snippet.java")))
                .thenReturn(highBuggy);

        // Register user
        RegisterRequest register = new RegisterRequest("batchuser", "batch@example.com", "Password123");
        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn();
        String token = objectMapper.readValue(regResult.getResponse().getContentAsString(), AuthResponse.class).getToken();

        // 1. Batch scan with 2 snippets
        CreateScanRequest batchRequest = new CreateScanRequest("BatchRepo", "http://git/batch", "main", "c123");
        batchRequest.setSnippets(List.of(
                new CreateScanRequest.SnippetItem("File1.java", "fn1", "snippet1"),
                new CreateScanRequest.SnippetItem("File2.java", "fn2", "snippet2")
        ));

        MvcResult batchResult = mockMvc.perform(post("/api/v1/scans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalFilesScanned").value(2))
                .andExpect(jsonPath("$.buggySnippetsFound").value(1))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andReturn();

        ScanResponseDto scanResponse = objectMapper.readValue(
                batchResult.getResponse().getContentAsString(),
                ScanResponseDto.class
        );
        Long scanId = scanResponse.getId();

        // 2. Summary counts add up
        mockMvc.perform(get("/api/v1/scans/" + scanId + "/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFindings").value(2))
                .andExpect(jsonPath("$.highRiskCount").value(1))
                .andExpect(jsonPath("$.lowRiskCount").value(1))
                .andExpect(jsonPath("$.mediumRiskCount").value(0))
                .andExpect(jsonPath("$.totalBuggyCount").value(1));

        // 3. Assert JSON key "isBuggy" on /scans/analyze (never "buggy")
        AnalyzeSnippetRequest singleSnippet = new AnalyzeSnippetRequest("snippet-single", "unknown", "Snippet.java", "fn");
        MvcResult analyzeResult = mockMvc.perform(post("/api/v1/scans/analyze")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(singleSnippet)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isBuggy").value(true))
                .andReturn();

        String json = analyzeResult.getResponse().getContentAsString();
        assertFalse(json.contains("\"buggy\":"), "JSON response must contain 'isBuggy' and never 'buggy': " + json);
    }
}
