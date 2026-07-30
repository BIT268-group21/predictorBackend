package com.tradingapp.auth;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tradingapp.AbstractIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Test
    void registerReturns201WithAWorkingToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "trader", "email", "Trader@Example.com",
                                "password", "supersecret"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInMs").value(3600000))
                .andExpect(jsonPath("$.user.email").value("trader@example.com"))
                .andExpect(jsonPath("$.user.username").value("trader"))
                .andExpect(jsonPath("$.user.role").value("USER"));
    }

    @Test
    void registerThenLoginReturnsAJwtThatOpensProtectedRoutes() throws Exception {
        registerAndGetToken("trader", "trader@example.com", "supersecret");

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "trader@example.com", "password", "supersecret"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(loginResponse).get("token").asText();

        mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("trader@example.com"));
    }

    @Test
    void duplicateEmailIsRejectedWith409() throws Exception {
        registerAndGetToken("trader", "trader@example.com", "supersecret");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "other", "email", "TRADER@example.com",
                                "password", "supersecret"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.path").value("/api/auth/register"));
    }

    @Test
    void invalidRegistrationPayloadIsRejectedWith400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "trader", "email", "not-an-email", "password", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void wrongPasswordIsRejectedWith401() throws Exception {
        registerAndGetToken("trader", "trader@example.com", "supersecret");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "trader@example.com", "password", "wrongpassword"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meRequiresAToken() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void meRejectsAGarbageToken() throws Exception {
        mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }
}
