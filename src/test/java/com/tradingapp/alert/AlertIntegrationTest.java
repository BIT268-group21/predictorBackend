package com.tradingapp.alert;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tradingapp.AbstractIntegrationTest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class AlertIntegrationTest extends AbstractIntegrationTest {

    private String token;

    @BeforeEach
    void registerCaller() throws Exception {
        token = registerAndGetToken("trader", "trader@example.com", "supersecret");
    }

    private static Map<String, Object> alertBody(String ticker, String price, String direction) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ticker", ticker);
        body.put("targetPrice", price);
        body.put("direction", direction);
        return body;
    }

    private long createAlert(String authToken, String ticker, String price, String direction) throws Exception {
        String response = mockMvc.perform(post("/api/alerts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(authToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(alertBody(ticker, price, direction))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void alertsRequireAToken() throws Exception {
        mockMvc.perform(get("/api/alerts"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(alertBody("AAPL", "200.00", "ABOVE"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createListAndDeleteRoundTrip() throws Exception {
        mockMvc.perform(post("/api/alerts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(alertBody(" aapl ", "200.50", "ABOVE"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticker").value("AAPL"))
                .andExpect(jsonPath("$.targetPrice").value(200.50))
                .andExpect(jsonPath("$.direction").value("ABOVE"))
                .andExpect(jsonPath("$.triggered").value(false))
                .andExpect(jsonPath("$.triggeredAt").doesNotExist());

        String listResponse = mockMvc.perform(get("/api/alerts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(listResponse).get(0).get("id").asLong();

        mockMvc.perform(delete("/api/alerts/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/alerts").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void invalidAlertPayloadIsRejectedWith400() throws Exception {
        mockMvc.perform(post("/api/alerts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(alertBody("AAPL", "-5", "ABOVE"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oneUserCannotSeeOrDeleteAnotherUsersAlert() throws Exception {
        long id = createAlert(token, "AAPL", "200.00", "ABOVE");
        String otherToken = registerAndGetToken("other", "other@example.com", "supersecret");

        mockMvc.perform(get("/api/alerts").header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(delete("/api/alerts/" + id).header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/alerts").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }
}
