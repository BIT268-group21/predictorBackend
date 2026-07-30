package com.tradingapp.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tradingapp.prediction.PredictorClient;
import com.tradingapp.prediction.dto.HistoryBar;
import com.tradingapp.prediction.dto.HistoryResponse;
import com.tradingapp.user.Role;
import com.tradingapp.user.User;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlertCheckerTest {

    private final User user = new User("trader", "trader@example.com", "hash", Role.USER);

    @Mock
    private AlertRepository alertRepository;
    @Mock
    private PredictorClient predictorClient;

    @InjectMocks
    private AlertChecker alertChecker;

    private static HistoryResponse lastClose(String ticker, double close) {
        return new HistoryResponse(ticker, 1,
                List.of(new HistoryBar("2026-07-24", close - 1, close + 1, close - 2, close, 1_000_000)));
    }

    @Test
    void flagsAlertWhosePriceCrossedAbove() {
        Alert alert = new Alert(user, "AAPL", new BigDecimal("200.00"), AlertDirection.ABOVE);
        when(alertRepository.findByTriggeredFalse()).thenReturn(List.of(alert));
        when(predictorClient.getHistory("AAPL", 1)).thenReturn(lastClose("AAPL", 211.8));

        alertChecker.checkAlerts();

        assertThat(alert.isTriggered()).isTrue();
        assertThat(alert.getTriggeredAt()).isNotNull();
        verify(alertRepository).save(alert);
    }

    @Test
    void flagsAlertWhosePriceCrossedBelow() {
        Alert alert = new Alert(user, "TSLA", new BigDecimal("250.00"), AlertDirection.BELOW);
        when(alertRepository.findByTriggeredFalse()).thenReturn(List.of(alert));
        when(predictorClient.getHistory("TSLA", 1)).thenReturn(lastClose("TSLA", 240.0));

        alertChecker.checkAlerts();

        assertThat(alert.isTriggered()).isTrue();
        verify(alertRepository).save(alert);
    }

    @Test
    void leavesAlertUntouchedWhenTargetIsNotReached() {
        Alert alert = new Alert(user, "AAPL", new BigDecimal("500.00"), AlertDirection.ABOVE);
        when(alertRepository.findByTriggeredFalse()).thenReturn(List.of(alert));
        when(predictorClient.getHistory("AAPL", 1)).thenReturn(lastClose("AAPL", 211.8));

        alertChecker.checkAlerts();

        assertThat(alert.isTriggered()).isFalse();
        assertThat(alert.getTriggeredAt()).isNull();
        verify(alertRepository, never()).save(alert);
    }

    @Test
    void aPriceLookupFailureDoesNotStopTheSweep() {
        Alert broken = new Alert(user, "NOPE", new BigDecimal("10.00"), AlertDirection.ABOVE);
        Alert healthy = new Alert(user, "AAPL", new BigDecimal("200.00"), AlertDirection.ABOVE);
        when(alertRepository.findByTriggeredFalse()).thenReturn(List.of(broken, healthy));
        when(predictorClient.getHistory("NOPE", 1)).thenThrow(new RuntimeException("boom"));
        when(predictorClient.getHistory("AAPL", 1)).thenReturn(lastClose("AAPL", 211.8));

        alertChecker.checkAlerts();

        assertThat(broken.isTriggered()).isFalse();
        assertThat(healthy.isTriggered()).isTrue();
        verify(alertRepository).save(healthy);
    }

    @Test
    void doesNothingWhenThereAreNoPendingAlerts() {
        when(alertRepository.findByTriggeredFalse()).thenReturn(List.of());

        alertChecker.checkAlerts();

        verify(predictorClient, never()).getHistory(anyString(), anyInt());
    }
}
