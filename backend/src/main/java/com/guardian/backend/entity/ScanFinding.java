package com.guardian.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scan_findings")
public class ScanFinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_scan_id", nullable = false)
    private RepositoryScan repositoryScan;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "function_name")
    private String functionName;

    @Lob
    @Column(name = "code_snippet", nullable = false, columnDefinition = "TEXT")
    private String codeSnippet;

    @Column(name = "risk_score", nullable = false)
    private double riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Column(name = "is_buggy", nullable = false)
    private boolean isBuggy;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Lob
    @Column(name = "suggested_fix", columnDefinition = "TEXT")
    private String suggestedFix;

    @Column(name = "retrieved_context_count")
    private int retrievedContextCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ScanFinding() {}

    public ScanFinding(String filePath, String functionName, String codeSnippet, double riskScore,
                       RiskLevel riskLevel, boolean isBuggy, String explanation, String suggestedFix,
                       int retrievedContextCount) {
        this.filePath = filePath;
        this.functionName = functionName;
        this.codeSnippet = codeSnippet;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.isBuggy = isBuggy;
        this.explanation = explanation;
        this.suggestedFix = suggestedFix;
        this.retrievedContextCount = retrievedContextCount;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RepositoryScan getRepositoryScan() {
        return repositoryScan;
    }

    public void setRepositoryScan(RepositoryScan repositoryScan) {
        this.repositoryScan = repositoryScan;
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

    public boolean isBuggy() {
        return isBuggy;
    }

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
