package com.guardian.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MlPredictRequest {

    @JsonProperty("code_snippet")
    private String codeSnippet;

    @JsonProperty("repo")
    private String repo = "unknown";

    @JsonProperty("filepath")
    private String filepath = "";

    public MlPredictRequest() {}

    public MlPredictRequest(String codeSnippet, String repo, String filepath) {
        this.codeSnippet = codeSnippet;
        this.repo = repo;
        this.filepath = filepath;
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
}
