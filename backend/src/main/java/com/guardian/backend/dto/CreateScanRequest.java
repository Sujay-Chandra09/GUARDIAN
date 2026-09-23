package com.guardian.backend.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

public class CreateScanRequest {

    @NotBlank(message = "Repository name is required")
    private String repoName;

    private String repoUrl;
    private String branch = "main";
    private String commitHash;

    // Optional list of initial snippets to analyze in this scan
    private List<SnippetItem> snippets = new ArrayList<>();

    public static class SnippetItem {
        private String filePath;
        private String functionName;
        private String codeSnippet;

        public SnippetItem() {}

        public SnippetItem(String filePath, String functionName, String codeSnippet) {
            this.filePath = filePath;
            this.functionName = functionName;
            this.codeSnippet = codeSnippet;
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
    }

    public CreateScanRequest() {}

    public CreateScanRequest(String repoName, String repoUrl, String branch, String commitHash) {
        this.repoName = repoName;
        this.repoUrl = repoUrl;
        this.branch = branch;
        this.commitHash = commitHash;
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

    public List<SnippetItem> getSnippets() {
        return snippets;
    }

    public void setSnippets(List<SnippetItem> snippets) {
        this.snippets = snippets;
    }
}
