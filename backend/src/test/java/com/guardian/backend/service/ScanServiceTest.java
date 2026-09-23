package com.guardian.backend.service;

import com.guardian.backend.dto.AnalyzeSnippetRequest;
import com.guardian.backend.dto.MlPredictResponse;
import com.guardian.backend.dto.ScanFindingDto;
import com.guardian.backend.dto.ScanSummaryDto;
import com.guardian.backend.entity.RiskLevel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("dev")
class ScanServiceTest {

    @Autowired
    private ScanService scanService;

    @MockBean
    private MlServiceClient mlServiceClient;

    @Test
    void shouldAnalyzeSnippetAndPersistFinding() {
        // Arrange mock ML response
        MlPredictResponse mockResponse = new MlPredictResponse();
        mockResponse.setRiskScore(0.9128);
        mockResponse.setBuggy(true);
        mockResponse.setRiskLevel("HIGH");
        mockResponse.setLatencyMs(45.5);
        mockResponse.setExplanation("NullPointer Exception Risk detected");
        mockResponse.setRetrievedContextCount(2);

        when(mlServiceClient.predictBugRisk(anyString(), anyString(), anyString()))
                .thenReturn(mockResponse);

        AnalyzeSnippetRequest request = new AnalyzeSnippetRequest(
                "public void test() { String s = null; s.length(); }",
                "Commons-Lang",
                "StringUtils.java",
                "test"
        );

        // Act
        ScanFindingDto finding = scanService.analyzeSnippet(request);

        // Assert
        assertNotNull(finding);
        assertNotNull(finding.getId());
        assertEquals("Commons-Lang", request.getRepo());
        assertEquals("StringUtils.java", finding.getFilePath());
        assertTrue(finding.isBuggy());
        assertEquals(0.9128, finding.getRiskScore());
        assertEquals(RiskLevel.HIGH, finding.getRiskLevel());
        assertEquals(2, finding.getRetrievedContextCount());

        // Verify summary
        ScanSummaryDto summary = scanService.getScanSummary(finding.getRepositoryScanId());
        assertNotNull(summary);
        assertEquals(1, summary.getTotalFindings());
        assertEquals(1, summary.getHighRiskCount());
        assertEquals("CRITICAL", summary.getOverallHealthStatus());
    }
}
