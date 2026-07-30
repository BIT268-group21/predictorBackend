package com.tradingapp.alert;

import com.tradingapp.prediction.PredictorClient;
import com.tradingapp.prediction.dto.HistoryBar;
import com.tradingapp.prediction.dto.HistoryResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Periodically compares the latest close of every un-triggered alert's ticker
 * against its target and flags the ones that crossed. Delivering a notification
 * is out of scope — flagging is enough.
 */
@Component
public class AlertChecker {

    private static final Logger log = LoggerFactory.getLogger(AlertChecker.class);

    private final AlertRepository alertRepository;
    private final PredictorClient predictorClient;

    public AlertChecker(AlertRepository alertRepository, PredictorClient predictorClient) {
        this.alertRepository = alertRepository;
        this.predictorClient = predictorClient;
    }

    @Scheduled(fixedDelayString = "${alerts.check-interval-ms:300000}",
            initialDelayString = "${alerts.initial-delay-ms:30000}")
    @Transactional
    public void checkAlerts() {
        List<Alert> pending = alertRepository.findByTriggeredFalse();
        if (pending.isEmpty()) {
            return;
        }

        Map<String, BigDecimal> latestCloses = new HashMap<>();
        int triggeredCount = 0;

        for (Alert alert : pending) {
            // One lookup per ticker per sweep, including the failures (null values).
            if (!latestCloses.containsKey(alert.getTicker())) {
                latestCloses.put(alert.getTicker(), latestClose(alert.getTicker()));
            }
            BigDecimal close = latestCloses.get(alert.getTicker());
            if (close != null && alert.isCrossedBy(close)) {
                alert.markTriggered(Instant.now());
                alertRepository.save(alert);
                triggeredCount++;
                log.info("Alert {} triggered: {} {} {} (last close {})", alert.getId(), alert.getTicker(),
                        alert.getDirection(), alert.getTargetPrice(), close);
            }
        }

        if (triggeredCount > 0) {
            log.info("Alert check flagged {} of {} pending alerts", triggeredCount, pending.size());
        }
    }

    /** Returns null when the price can't be fetched — one bad ticker must not stop the sweep. */
    private BigDecimal latestClose(String ticker) {
        try {
            HistoryResponse history = predictorClient.getHistory(ticker, 1);
            List<HistoryBar> bars = history == null ? null : history.ohlcv();
            if (bars == null || bars.isEmpty()) {
                return null;
            }
            return BigDecimal.valueOf(bars.get(bars.size() - 1).close());
        } catch (RuntimeException ex) {
            log.warn("Could not fetch latest close for {}: {}", ticker, ex.getMessage());
            return null;
        }
    }
}
