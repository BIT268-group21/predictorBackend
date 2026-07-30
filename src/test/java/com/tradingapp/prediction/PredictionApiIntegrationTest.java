package com.tradingapp.prediction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tradingapp.AbstractIntegrationTest;
import com.tradingapp.common.UpstreamException;
import com.tradingapp.prediction.dto.HistoryBar;
import com.tradingapp.prediction.dto.HistoryResponse;
import com.tradingapp.prediction.dto.PredictRequest;
import com.tradingapp.prediction.dto.PredictResponse;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

class PredictionApiIntegrationTest extends AbstractIntegrationTest {

    private static HistoryResponse history(String ticker, int rows) {
        List<HistoryBar> bars = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            double base = 100 + i;
            bars.add(new HistoryBar("2026-01-01", base, base + 2, base - 2, base + 1, 1_000_000));
        }
        return new HistoryResponse(ticker, rows, bars);
    }

    private void stubSuccessfulPrediction() {
        when(predictorClient.getHistory(anyString(), anyInt())).thenAnswer(
                invocation -> history(invocation.getArgument(0), 60));
        when(predictorClient.predict(any(PredictRequest.class))).thenReturn(new PredictResponse(
                "req-1", "AAPL", "success", 5, "BULLISH", 0.53,
                List.of("bullish_engulfing"), "2026-07-24T13:13:21Z", null));
    }

    @Test
    void stockCatalogIsPublic() throws Exception {
        mockMvc.perform(get("/api/stocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].ticker").isNotEmpty())
                .andExpect(jsonPath("$[0].name").isNotEmpty());
    }

    @Test
    void stockHistoryIsPublicAndProxiesThePredictor() throws Exception {
        when(predictorClient.getHistory(eq("AAPL"), eq(10))).thenReturn(history("AAPL", 10));

        mockMvc.perform(get("/api/stocks/aapl/history?limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker").value("AAPL"))
                .andExpect(jsonPath("$.rows").value(10))
                .andExpect(jsonPath("$.ohlcv", hasSize(10)))
                .andExpect(jsonPath("$.ohlcv[0].date").isNotEmpty())
                .andExpect(jsonPath("$.ohlcv[0].close").isNumber());
    }

    @Test
    void anonymousPredictionSucceedsAndIsNotLogged() throws Exception {
        stubSuccessfulPrediction();

        mockMvc.perform(get("/api/predict/aapl"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker").value("AAPL"))
                .andExpect(jsonPath("$.trend").value("BULLISH"))
                .andExpect(jsonPath("$.confidence").value(0.53))
                .andExpect(jsonPath("$.horizonDays").value(5))
                .andExpect(jsonPath("$.detectedPatterns", hasSize(1)))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());

        assertThat(predictionLogRepository.count()).isZero();
    }

    @Test
    void authenticatedPredictionIsAlsoLoggedToTheUsersHistory() throws Exception {
        stubSuccessfulPrediction();
        String token = registerAndGetToken("trader", "trader@example.com", "supersecret");

        mockMvc.perform(get("/api/predict/AAPL").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trend").value("BULLISH"));

        assertThat(predictionLogRepository.count()).isEqualTo(1);

        mockMvc.perform(get("/api/predictions/history").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ticker").value("AAPL"))
                .andExpect(jsonPath("$[0].trend").value("BULLISH"))
                .andExpect(jsonPath("$[0].detectedPatterns[0]").value("bullish_engulfing"));
    }

    @Test
    void predictionHistoryOnlyEverReturnsTheCallersRows() throws Exception {
        stubSuccessfulPrediction();
        String token = registerAndGetToken("trader", "trader@example.com", "supersecret");
        String otherToken = registerAndGetToken("other", "other@example.com", "supersecret");

        mockMvc.perform(get("/api/predict/AAPL").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/predictions/history").header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/predictions/history").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void predictionHistoryRequiresAToken() throws Exception {
        mockMvc.perform(get("/api/predictions/history"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anInvalidTokenStillLeavesThePublicPredictEndpointUsable() throws Exception {
        stubSuccessfulPrediction();

        mockMvc.perform(get("/api/predict/AAPL").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isOk());

        assertThat(predictionLogRepository.count()).isZero();
    }

    @Test
    void insufficientHistoryIsReportedAs422() throws Exception {
        when(predictorClient.getHistory(anyString(), anyInt())).thenReturn(history("AAPL", 10));

        mockMvc.perform(get("/api/predict/AAPL"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.path").value("/api/predict/AAPL"));
    }

    @Test
    void predictorErrorPayloadIsReportedAs422WithItsDetail() throws Exception {
        when(predictorClient.getHistory(anyString(), anyInt())).thenReturn(history("AAPL", 60));
        when(predictorClient.predict(any(PredictRequest.class))).thenReturn(new PredictResponse(
                "req-1", "AAPL", "error", 5, "UNKNOWN", 0.0, List.of(), "2026-07-24T13:13:21Z",
                "insufficient history: need at least 40 bars, got 12"));

        mockMvc.perform(get("/api/predict/AAPL"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("insufficient history: need at least 40 bars, got 12"));
    }

    @Test
    void predictorOutageIsReportedAs503() throws Exception {
        when(predictorClient.getHistory(anyString(), anyInt()))
                .thenThrow(new UpstreamException(HttpStatus.SERVICE_UNAVAILABLE, "prediction service unavailable"));

        mockMvc.perform(get("/api/predict/AAPL"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("prediction service unavailable"));
    }

    @Test
    void corsPreflightAllowsTheConfiguredOriginAndAuthorizationHeader() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .options("/api/watchlist")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));
    }
}
