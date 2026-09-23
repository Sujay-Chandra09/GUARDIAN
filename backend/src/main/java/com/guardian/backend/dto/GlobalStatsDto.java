package com.guardian.backend.dto;

public class GlobalStatsDto {

    private long totalScans;
    private long totalBugsDetected;
    private double overallAverageRiskScore;
    private long activeScans;

    public GlobalStatsDto() {}

    public GlobalStatsDto(long totalScans, long totalBugsDetected, double overallAverageRiskScore, long activeScans) {
        this.totalScans = totalScans;
        this.totalBugsDetected = totalBugsDetected;
        this.overallAverageRiskScore = overallAverageRiskScore;
        this.activeScans = activeScans;
    }

    public long getTotalScans() {
        return totalScans;
    }

    public void setTotalScans(long totalScans) {
        this.totalScans = totalScans;
    }

    public long getTotalBugsDetected() {
        return totalBugsDetected;
    }

    public void setTotalBugsDetected(long totalBugsDetected) {
        this.totalBugsDetected = totalBugsDetected;
    }

    public double getOverallAverageRiskScore() {
        return overallAverageRiskScore;
    }

    public void setOverallAverageRiskScore(double overallAverageRiskScore) {
        this.overallAverageRiskScore = overallAverageRiskScore;
    }

    public long getActiveScans() {
        return activeScans;
    }

    public void setActiveScans(long activeScans) {
        this.activeScans = activeScans;
    }
}
