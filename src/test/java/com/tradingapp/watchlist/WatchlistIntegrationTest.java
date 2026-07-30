package com.tradingapp.watchlist;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tradingapp.AbstractIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class WatchlistIntegrationTest extends AbstractIntegrationTest {

    private String token;

    @BeforeEach
    void registerCaller() throws Exception {
        token = registerAndGetToken("trader", "trader@example.com", "supersecret");
    }

    @Test
    void watchlistRequiresAToken() throws Exception {
        mockMvc.perform(get("/api/watchlist"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/watchlist").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void addListAndDeleteRoundTrip() throws Exception {
        mockMvc.perform(post("/api/watchlist")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("ticker", " aapl "))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticker").value("AAPL"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        mockMvc.perform(get("/api/watchlist").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ticker").value("AAPL"));

        mockMvc.perform(delete("/api/watchlist/aapl").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/watchlist").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void duplicateTickerIsRejectedWith409() throws Exception {
        mockMvc.perform(post("/api/watchlist")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("ticker", "AAPL"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/watchlist")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("ticker", "aapl"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void blankTickerIsRejectedWith400() throws Exception {
        mockMvc.perform(post("/api/watchlist")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("ticker", "  "))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oneUserNeverSeesOrDeletesAnotherUsersItems() throws Exception {
        String otherToken = registerAndGetToken("other", "other@example.com", "supersecret");

        mockMvc.perform(post("/api/watchlist")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("ticker", "AAPL"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/watchlist").header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // The other user may add the same ticker for themselves ...
        mockMvc.perform(post("/api/watchlist")
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("ticker", "AAPL"))))
                .andExpect(status().isCreated());

        // ... and deleting theirs leaves the first user's row alone.
        mockMvc.perform(delete("/api/watchlist/AAPL").header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/watchlist").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ticker").value("AAPL"));
    }

    @Test
    void deletingATickerThatIsNotOnTheListReturns404() throws Exception {
        mockMvc.perform(delete("/api/watchlist/NFLX").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
    }
}
