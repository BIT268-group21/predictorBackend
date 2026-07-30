package com.tradingapp.prediction;

import com.tradingapp.common.BadRequestException;
import com.tradingapp.common.NotFoundException;
import com.tradingapp.common.Tickers;
import com.tradingapp.common.UpstreamException;
import com.tradingapp.prediction.dto.HistoryBar;
import com.tradingapp.prediction.dto.HistoryResponse;
import com.tradingapp.prediction.dto.OhlcvBar;
import com.tradingapp.prediction.dto.PredictRequest;
import com.tradingapp.prediction.dto.PredictResponse;
import com.tradingapp.prediction.dto.PredictionHistoryItem;
import com.tradingapp.prediction.dto.PredictionResult;
import com.tradingapp.user.User;
import com.tradingapp.user.UserRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Orchestrates the prediction flow described in BUILD_SPEC §3.4. */
@Service
public class PredictionService {

    private static final Logger log = LoggerFactory.getLogger(PredictionService.class);

    /** The model needs 40 bars; we fetch a comfortable margin. */
    static final int MIN_BARS = 40;
    static final int HISTORY_LIMIT = 60;
    private static final int MAX_HISTORY_LIMIT = 1000;

    private final PredictorClient predictorClient;
    private final PredictionLogRepository predictionLogRepository;
    private final UserRepository userRepository;

    public PredictionService(PredictorClient predictorClient,
                             PredictionLogRepository predictionLogRepository,
                             UserRepository userRepository) {
        this.predictorClient = predictorClient;
        this.predictionLogRepository = predictionLogRepository;
        this.userRepository = userRepository;
    }

    /**
     * @param userEmail the authenticated caller, or {@code null} for an anonymous
     *                  request (anonymous predictions are not logged).
     */
    @Transactional
    public PredictionResult predict(String rawTicker, String userEmail) {
        String ticker = Tickers.normalize(rawTicker);

        HistoryResponse history = predictorClient.getHistory(ticker, HISTORY_LIMIT);
        List<HistoryBar> bars = history == null ? null : history.ohlcv();
        if (bars == null || bars.size() < MIN_BARS) {
            int rows = bars == null ? 0 : bars.size();
            throw new UpstreamException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "not enough history to predict: need at least " + MIN_BARS + " bars, got " + rows);
        }

        List<OhlcvBar> ohlcv = bars.stream()
                .map(bar -> new OhlcvBar(bar.open(), bar.high(), bar.low(), bar.close(), bar.volume()))
                .toList();

        PredictResponse response = predictorClient.predict(
                new PredictRequest(UUID.randomUUID().toString(), ticker, ohlcv));

        if (response == null) {
            throw new UpstreamException(HttpStatus.BAD_GATEWAY, "empty response from prediction service");
        }
        if (!"success".equalsIgnoreCase(response.status())) {
            String detail = (response.detail() == null || response.detail().isBlank())
                    ? "prediction failed"
                    : response.detail();
            throw new UpstreamException(HttpStatus.UNPROCESSABLE_ENTITY, detail);
        }

        List<String> patterns = response.detected_patterns() == null
                ? List.of()
                : List.copyOf(response.detected_patterns());

        PredictionResult result = new PredictionResult(
                ticker,
                response.trend_classification(),
                response.confidence_score(),
                response.horizon_days(),
                patterns,
                parseTimestamp(response.timestamp()));

        if (userEmail != null) {
            logPrediction(userEmail, result);
        }
        return result;
    }

    /** Proxies the predictor's OHLCV history for the public stock endpoints. */
    public HistoryResponse history(String rawTicker, int limit) {
        String ticker = Tickers.normalize(rawTicker);
        if (limit < 1) {
            throw new BadRequestException("limit must be at least 1");
        }
        HistoryResponse history = predictorClient.getHistory(ticker, Math.min(limit, MAX_HISTORY_LIMIT));
        if (history == null) {
            throw new NotFoundException("no price history for " + ticker);
        }
        return history;
    }

    @Transactional(readOnly = true)
    public List<PredictionHistoryItem> historyForUser(String userEmail) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new NotFoundException("user not found"));
        return predictionLogRepository.findByUserIdOrderByCreatedAtDescIdDesc(user.getId()).stream()
                .map(PredictionHistoryItem::from)
                .toList();
    }

    private void logPrediction(String userEmail, PredictionResult result) {
        Optional<User> user = userRepository.findByEmailIgnoreCase(userEmail);
        if (user.isEmpty()) {
            log.warn("Skipping prediction log: no user for {}", userEmail);
            return;
        }
        predictionLogRepository.save(new PredictionLog(
                user.get(),
                result.ticker(),
                result.trend(),
                result.confidence(),
                result.horizonDays(),
                result.detectedPatterns()));
    }

    private static Instant parseTimestamp(String raw) {
        if (raw == null || raw.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(raw);
        } catch (DateTimeParseException ex) {
            try {
                return OffsetDateTime.parse(raw).toInstant();
            } catch (DateTimeParseException nested) {
                log.debug("Unparseable predictor timestamp '{}', falling back to now", raw);
                return Instant.now();
            }
        }
    }
}
