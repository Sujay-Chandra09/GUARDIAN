package com.guardian.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MlPredictResponse {

    @JsonProperty("risk_score")
    private double riskScore;

    @JsonProperty("is_buggy")
    private boolean isBuggy;

    @JsonProperty("risk_level")
    private String riskLevel;

    @JsonProperty("latency_ms")
    private double latencyMs;

    @JsonProperty("explanation")
    private String explanation;

    @JsonProperty("retrieved_context_count")
    private int retrievedContextCount;

    public MlPredictResponse() {}

    public double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(double riskScore) {
        this.riskScore = riskScore;
    }

    public boolean isBuggy() {
        return isBuggy;
    }

    public void setBuggy(boolean buggy) {
        isBuggy = buggy;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public double getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(double latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public int getRetrievedContextCount() {
        return retrievedContextCount;
    }

    public void setRetrievedContextCount(int retrievedContextCount) {
        this.retrievedContextCount = retrievedContextCount;
    }
}
