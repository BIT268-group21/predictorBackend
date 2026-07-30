package com.tradingapp.prediction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tradingapp.common.UpstreamException;
import com.tradingapp.prediction.dto.HistoryBar;
import com.tradingapp.prediction.dto.HistoryResponse;
import com.tradingapp.prediction.dto.PredictRequest;
import com.tradingapp.prediction.dto.PredictResponse;
import com.tradingapp.prediction.dto.PredictionResult;
import com.tradingapp.user.Role;
import com.tradingapp.user.User;
import com.tradingapp.user.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class PredictionServiceTest {

    private static final String EMAIL = "trader@example.com";

    @Mock
    private PredictorClient predictorClient;
    @Mock
    private PredictionLogRepository predictionLogRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PredictionService predictionService;

    private static HistoryResponse history(String ticker, int rows) {
        List<HistoryBar> bars = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            double base = 100 + i;
            bars.add(new HistoryBar("2026-01-" + String.format("%02d", (i % 28) + 1),
                    base, base + 2, base - 2, base + 1, 1_000_000 + i));
        }
        return new HistoryResponse(ticker, rows, bars);
    }

    private static PredictResponse success() {
        return new PredictResponse("req-1", "AAPL", "success", 5, "BULLISH", 0.53,
                List.of("bullish_engulfing"), "2026-07-24T13:13:21Z", null);
    }

    @Test
    void authenticatedPredictionReturnsResultAndIsLogged() {
        User user = new User("trader", EMAIL, "hash", Role.USER);
        when(predictorClient.getHistory("AAPL", PredictionService.HISTORY_LIMIT)).thenReturn(history("AAPL", 60));
        when(predictorClient.predict(any(PredictRequest.class))).thenReturn(success());
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));

        PredictionResult result = predictionService.predict("aapl", EMAIL);

        assertThat(result.ticker()).isEqualTo("AAPL");
        assertThat(result.trend()).isEqualTo("BULLISH");
        assertThat(result.confidence()).isEqualTo(0.53);
        assertThat(result.horizonDays()).isEqualTo(5);
        assertThat(result.detectedPatterns()).containsExactly("bullish_engulfing");

        ArgumentCaptor<PredictionLog> captor = ArgumentCaptor.forClass(PredictionLog.class);
        verify(predictionLogRepository).save(captor.capture());
        PredictionLog saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getTicker()).isEqualTo("AAPL");
        assertThat(saved.getTrendClassification()).isEqualTo("BULLISH");
        assertThat(saved.getConfidenceScore()).isEqualTo(0.53);
        assertThat(saved.getHorizonDays()).isEqualTo(5);
        assertThat(saved.getDetectedPatternsAsList()).containsExactly("bullish_engulfing");
    }

    @Test
    void forwardsExactlyTheBarsFromHistoryWithoutTheDateField() {
        when(predictorClient.getHistory("AAPL", PredictionService.HISTORY_LIMIT)).thenReturn(history("AAPL", 60));
        when(predictorClient.predict(any(PredictRequest.class))).thenReturn(success());

        predictionService.predict("AAPL", null);

        ArgumentCaptor<PredictRequest> captor = ArgumentCaptor.forClass(PredictRequest.class);
        verify(predictorClient).predict(captor.capture());
        PredictRequest request = captor.getValue();
        assertThat(request.request_id()).isNotBlank();
        assertThat(request.ticker()).isEqualTo("AAPL");
        assertThat(request.ohlcv()).hasSize(60);
        // order preserved, oldest first
        assertThat(request.ohlcv().get(0).open()).isEqualTo(100.0);
        assertThat(request.ohlcv().get(59).open()).isEqualTo(159.0);
    }

    @Test
    void anonymousPredictionIsNotLogged() {
        when(predictorClient.getHistory("TSLA", PredictionService.HISTORY_LIMIT)).thenReturn(history("TSLA", 45));
        when(predictorClient.predict(any(PredictRequest.class))).thenReturn(success());

        PredictionResult result = predictionService.predict("tsla", null);

        assertThat(result.ticker()).isEqualTo("TSLA");
        verifyNoInteractions(predictionLogRepository);
        verifyNoInteractions(userRepository);
    }

    @Test
    void insufficientHistoryIsRejectedWith422AndNeverCallsThePredictor() {
        when(predictorClient.getHistory(eq("AAPL"), anyInt())).thenReturn(history("AAPL", 12));

        assertThatThrownBy(() -> predictionService.predict("AAPL", EMAIL))
                .isInstanceOf(UpstreamException.class)
                .satisfies(ex -> assertThat(((UpstreamException) ex).getStatus())
                        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY))
                .hasMessageContaining("not enough history");

        verify(predictorClient, never()).predict(any());
        verifyNoInteractions(predictionLogRepository);
    }

    @Test
    void predictorErrorStatusIsSurfacedAs422WithItsDetail() {
        when(predictorClient.getHistory(eq("AAPL"), anyInt())).thenReturn(history("AAPL", 60));
        when(predictorClient.predict(any(PredictRequest.class))).thenReturn(new PredictResponse(
                "req-1", "AAPL", "error", 5, "UNKNOWN", 0.0, List.of(), "2026-07-24T13:13:21Z",
                "insufficient history: need at least 40 bars, got 12"));

        assertThatThrownBy(() -> predictionService.predict("AAPL", EMAIL))
                .isInstanceOf(UpstreamException.class)
                .satisfies(ex -> assertThat(((UpstreamException) ex).getStatus())
                        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY))
                .hasMessage("insufficient history: need at least 40 bars, got 12");

        verifyNoInteractions(predictionLogRepository);
    }

    @Test
    void predictorOutageIsPropagatedAs503() {
        when(predictorClient.getHistory(anyString(), anyInt()))
                .thenThrow(new UpstreamException(HttpStatus.SERVICE_UNAVAILABLE, "prediction service unavailable"));

        assertThatThrownBy(() -> predictionService.predict("AAPL", null))
                .isInstanceOf(UpstreamException.class)
                .satisfies(ex -> assertThat(((UpstreamException) ex).getStatus())
                        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void emptyHistoryPayloadIsRejectedWith422() {
        when(predictorClient.getHistory(eq("AAPL"), anyInt())).thenReturn(new HistoryResponse("AAPL", 0, null));

        assertThatThrownBy(() -> predictionService.predict("AAPL", null))
                .isInstanceOf(UpstreamException.class)
                .satisfies(ex -> assertThat(((UpstreamException) ex).getStatus())
                        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }
}
