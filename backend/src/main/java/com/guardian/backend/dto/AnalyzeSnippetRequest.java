package com.guardian.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class AnalyzeSnippetRequest {

    @NotBlank(message = "Code snippet is required")
    private String codeSnippet;

    private String repo = "unknown";
    private String filepath = "Snippet.java";
    private String functionName = "anonymous";
    private Long repositoryScanId; // Optional: associate with an existing scan

    public AnalyzeSnippetRequest() {}

    public AnalyzeSnippetRequest(String codeSnippet, String repo, String filepath, String functionName) {
        this.codeSnippet = codeSnippet;
        this.repo = repo;
        this.filepath = filepath;
        this.functionName = functionName;
    }

    public String getCodeSnippet() {
        return codeSnippet;
    }

    public void setCodeSnippet(String codeSnippet) {
        this.codeSnippet = codeSnippet;
    }

    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public Long getRepositoryScanId() {
        return repositoryScanId;
    }

    public void setRepositoryScanId(Long repositoryScanId) {
        this.repositoryScanId = repositoryScanId;
    }
}
