package com.guardian.backend.dto;

public class ScanSummaryDto {

    private Long scanId;
    private String repoName;
    private int totalFindings;
    private long highRiskCount;
    private long mediumRiskCount;
    private long lowRiskCount;
    private long totalBuggyCount;
    private double averageRiskScore;
    private String overallHealthStatus; // "HEALTHY", "WARNING", "CRITICAL"

    public ScanSummaryDto() {}

    public Long getScanId() {
        return scanId;
    }

    public void setScanId(Long scanId) {
        this.scanId = scanId;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public int getTotalFindings() {
        return totalFindings;
    }

    public void setTotalFindings(int totalFindings) {
        this.totalFindings = totalFindings;
    }

    public long getHighRiskCount() {
        return highRiskCount;
    }

    public void setHighRiskCount(long highRiskCount) {
        this.highRiskCount = highRiskCount;
    }

    public long getMediumRiskCount() {
        return mediumRiskCount;
    }

    public void setMediumRiskCount(long mediumRiskCount) {
        this.mediumRiskCount = mediumRiskCount;
    }

    public long getLowRiskCount() {
        return lowRiskCount;
    }

    public void setLowRiskCount(long lowRiskCount) {
        this.lowRiskCount = lowRiskCount;
    }

    public long getTotalBuggyCount() {
        return totalBuggyCount;
    }

    public void setTotalBuggyCount(long totalBuggyCount) {
        this.totalBuggyCount = totalBuggyCount;
    }

    public double getAverageRiskScore() {
        return averageRiskScore;
    }

    public void setAverageRiskScore(double averageRiskScore) {
        this.averageRiskScore = averageRiskScore;
    }

    public String getOverallHealthStatus() {
        return overallHealthStatus;
    }

    public void setOverallHealthStatus(String overallHealthStatus) {
        this.overallHealthStatus = overallHealthStatus;
    }
}
