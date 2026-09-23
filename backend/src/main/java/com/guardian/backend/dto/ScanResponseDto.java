package com.guardian.backend.dto;

import com.guardian.backend.entity.RepositoryScan;
import com.guardian.backend.entity.ScanStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ScanResponseDto {

    private Long id;
    private String repoName;
    private String repoUrl;
    private String branch;
    private String commitHash;
    private ScanStatus status;
    private int totalFilesScanned;
    private int buggySnippetsFound;
    private double averageRiskScore;
    private long scanDurationMs;
    private LocalDateTime createdAt;
    private Long userId;
    private String username;
    private List<ScanFindingDto> findings = new ArrayList<>();

    public ScanResponseDto() {}

    public static ScanResponseDto fromEntity(RepositoryScan scan, boolean includeFindings) {
        ScanResponseDto dto = new ScanResponseDto();
        dto.setId(scan.getId());
        dto.setRepoName(scan.getRepoName());
        dto.setRepoUrl(scan.getRepoUrl());
        dto.setBranch(scan.getBranch());
        dto.setCommitHash(scan.getCommitHash());
        dto.setStatus(scan.getStatus());
        dto.setTotalFilesScanned(scan.getTotalFilesScanned());
        dto.setBuggySnippetsFound(scan.getBuggySnippetsFound());
        dto.setAverageRiskScore(scan.getAverageRiskScore());
        dto.setScanDurationMs(scan.getScanDurationMs());
        dto.setCreatedAt(scan.getCreatedAt());

        if (scan.getUser() != null) {
            dto.setUserId(scan.getUser().getId());
            dto.setUsername(scan.getUser().getUsername());
        }

        if (includeFindings && scan.getFindings() != null) {
            dto.setFindings(
                scan.getFindings().stream()
                    .map(ScanFindingDto::fromEntity)
                    .collect(Collectors.toList())
            );
        }
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public void setCommitHash(String commitHash) {
        this.commitHash = commitHash;
    }

    public ScanStatus getStatus() {
        return status;
    }

    public void setStatus(ScanStatus status) {
        this.status = status;
    }

    public int getTotalFilesScanned() {
        return totalFilesScanned;
    }

    public void setTotalFilesScanned(int totalFilesScanned) {
        this.totalFilesScanned = totalFilesScanned;
    }

    public int getBuggySnippetsFound() {
        return buggySnippetsFound;
    }

    public void setBuggySnippetsFound(int buggySnippetsFound) {
        this.buggySnippetsFound = buggySnippetsFound;
    }

    public double getAverageRiskScore() {
        return averageRiskScore;
    }

    public void setAverageRiskScore(double averageRiskScore) {
        this.averageRiskScore = averageRiskScore;
    }

    public long getScanDurationMs() {
        return scanDurationMs;
    }

    public void setScanDurationMs(long scanDurationMs) {
        this.scanDurationMs = scanDurationMs;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<ScanFindingDto> getFindings() {
        return findings;
    }

    public void setFindings(List<ScanFindingDto> findings) {
        this.findings = findings;
    }
}
