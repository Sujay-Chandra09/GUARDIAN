package com.guardian.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "repository_scans")
public class RepositoryScan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repo_name", nullable = false)
    private String repoName;

    @Column(name = "repo_url")
    private String repoUrl;

    @Column(length = 100)
    private String branch = "main";

    @Column(name = "commit_hash", length = 64)
    private String commitHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScanStatus status = ScanStatus.PENDING;

    @Column(name = "total_files_scanned")
    private int totalFilesScanned = 0;

    @Column(name = "buggy_snippets_found")
    private int buggySnippetsFound = 0;

    @Column(name = "average_risk_score")
    private double averageRiskScore = 0.0;

    @Column(name = "scan_duration_ms")
    private long scanDurationMs = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "repositoryScan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScanFinding> findings = new ArrayList<>();

    public RepositoryScan() {}

    public RepositoryScan(String repoName, String repoUrl, String branch, String commitHash, User user) {
        this.repoName = repoName;
        this.repoUrl = repoUrl;
        this.branch = branch != null ? branch : "main";
        this.commitHash = commitHash;
        this.user = user;
        this.createdAt = LocalDateTime.now();
        this.status = ScanStatus.PENDING;
    }

    public void addFinding(ScanFinding finding) {
        findings.add(finding);
        finding.setRepositoryScan(this);
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<ScanFinding> getFindings() {
        return findings;
    }

    public void setFindings(List<ScanFinding> findings) {
        this.findings = findings;
    }
}
