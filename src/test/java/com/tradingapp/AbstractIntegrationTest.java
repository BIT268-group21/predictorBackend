package com.tradingapp;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingapp.alert.AlertRepository;
import com.tradingapp.prediction.PredictionLogRepository;
import com.tradingapp.prediction.PredictorClient;
import com.tradingapp.user.UserRepository;
import com.tradingapp.watchlist.WatchlistRepository;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Shared wiring for the MockMvc slices: H2, a mocked predictor and a clean database per test. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;

    @MockBean
    protected PredictorClient predictorClient;

    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected WatchlistRepository watchlistRepository;
    @Autowired
    protected PredictionLogRepository predictionLogRepository;
    @Autowired
    protected AlertRepository alertRepository;

    @BeforeEach
    void resetDatabase() {
        watchlistRepository.deleteAll();
        predictionLogRepository.deleteAll();
        alertRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    /** Registers a user and returns the bearer token from the auto-login response. */
    protected String registerAndGetToken(String username, String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "email", email, "password", password))))
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(response);
        return node.get("token").asText();
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }
}
