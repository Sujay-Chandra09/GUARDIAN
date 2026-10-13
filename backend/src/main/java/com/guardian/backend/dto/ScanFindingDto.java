package com.guardian.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.guardian.backend.entity.RiskLevel;
import com.guardian.backend.entity.ScanFinding;
import java.time.LocalDateTime;

public class ScanFindingDto {

    private Long id;
    private Long repositoryScanId;
    private String filePath;
    private String functionName;
    private String codeSnippet;
    private double riskScore;
    private RiskLevel riskLevel;

    @JsonProperty("isBuggy")
    private boolean isBuggy;

    private String explanation;
    private String suggestedFix;
    private int retrievedContextCount;
    private LocalDateTime createdAt;

    public ScanFindingDto() {}

    public static ScanFindingDto fromEntity(ScanFinding finding) {
        ScanFindingDto dto = new ScanFindingDto();
        dto.setId(finding.getId());
        if (finding.getRepositoryScan() != null) {
            dto.setRepositoryScanId(finding.getRepositoryScan().getId());
        }
        dto.setFilePath(finding.getFilePath());
        dto.setFunctionName(finding.getFunctionName());
        dto.setCodeSnippet(finding.getCodeSnippet());
        dto.setRiskScore(finding.getRiskScore());
        dto.setRiskLevel(finding.getRiskLevel());
        dto.setBuggy(finding.isBuggy());
        dto.setExplanation(finding.getExplanation());
        dto.setSuggestedFix(finding.getSuggestedFix());
        dto.setRetrievedContextCount(finding.getRetrievedContextCount());
        dto.setCreatedAt(finding.getCreatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRepositoryScanId() {
        return repositoryScanId;
    }

    public void setRepositoryScanId(Long repositoryScanId) {
        this.repositoryScanId = repositoryScanId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public String getCodeSnippet() {
        return codeSnippet;
    }

    public void setCodeSnippet(String codeSnippet) {
        this.codeSnippet = codeSnippet;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(double riskScore) {
        this.riskScore = riskScore;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    @JsonProperty("isBuggy")
    public boolean isBuggy() {
        return isBuggy;
    }

    @JsonProperty("isBuggy")
    public void setBuggy(boolean buggy) {
        isBuggy = buggy;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getSuggestedFix() {
        return suggestedFix;
    }

    public void setSuggestedFix(String suggestedFix) {
        this.suggestedFix = suggestedFix;
    }

    public int getRetrievedContextCount() {
        return retrievedContextCount;
    }

    public void setRetrievedContextCount(int retrievedContextCount) {
        this.retrievedContextCount = retrievedContextCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
