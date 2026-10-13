package com.guardian.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guardian.backend.dto.AnalyzeSnippetRequest;
import com.guardian.backend.dto.AuthRequest;
import com.guardian.backend.dto.AuthResponse;
import com.guardian.backend.dto.CreateScanRequest;
import com.guardian.backend.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testValidationAndErrorHandling() throws Exception {
        // 1. Short password -> 400 with fieldErrors
        RegisterRequest shortPassword = new RegisterRequest("valuser1", "val1@example.com", "short1");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shortPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").isNotEmpty());

        // 2. Bad email -> 400 with fieldErrors
        RegisterRequest badEmail = new RegisterRequest("valuser2", "invalid-email-format", "ValidPass123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badEmail)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").isNotEmpty());

        // 3. Register valid user to get token for scan tests
        RegisterRequest validUser = new RegisterRequest("valuser3", "val3@example.com", "ValidPass123");
        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUser)))
                .andExpect(status().isCreated())
                .andReturn();
        String token = objectMapper.readValue(regResult.getResponse().getContentAsString(), AuthResponse.class).getToken();

        // 4. Duplicate username -> 409
        RegisterRequest duplicateUser = new RegisterRequest("valuser3", "other@example.com", "ValidPass123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateUser)))
                .andExpect(status().isConflict());

        // 5. Blank snippet -> 400 with fieldErrors
        AnalyzeSnippetRequest blankSnippet = new AnalyzeSnippetRequest("", "Repo", "File.java", "test");
        mockMvc.perform(post("/api/v1/scans/analyze")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankSnippet)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.codeSnippet").isNotEmpty());

        // 6. Batch scan with snippet without filePath -> 400 with fieldErrors
        CreateScanRequest batchMissingPath = new CreateScanRequest("Repo", "http://git", "main", "c1");
        CreateScanRequest.SnippetItem itemWithoutPath = new CreateScanRequest.SnippetItem("", "fn", "System.out.println();");
        batchMissingPath.setSnippets(List.of(itemWithoutPath));

        mockMvc.perform(post("/api/v1/scans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchMissingPath)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isNotEmpty());

        // 7. /scans/abc -> 400
        mockMvc.perform(get("/api/v1/scans/abc")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        // 8. Unknown route -> 404
        mockMvc.perform(get("/api/v1/unknown-route")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        // 9. Wrong method -> 405
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/health"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("Method Not Allowed"));

        // 10. Wrong password -> 401
        AuthRequest wrongPass = new AuthRequest("valuser3", "WrongPassword999");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPass)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
