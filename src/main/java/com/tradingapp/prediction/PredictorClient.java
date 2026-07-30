package com.tradingapp.prediction;

import com.tradingapp.common.ApiException;
import com.tradingapp.common.NotFoundException;
import com.tradingapp.common.UpstreamException;
import com.tradingapp.prediction.dto.HistoryResponse;
import com.tradingapp.prediction.dto.PredictRequest;
import com.tradingapp.prediction.dto.PredictResponse;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * The only client of the Python prediction microservice (BUILD_SPEC §3).
 * Connection failures and timeouts are retried once — the predictor may be
 * cold-starting — and every failure is translated into the status the frontend
 * should see.
 */
@Component
public class PredictorClient {

    private static final Logger log = LoggerFactory.getLogger(PredictorClient.class);

    private final RestClient client;

    public PredictorClient(RestClient predictorRestClient) {
        this.client = predictorRestClient;
    }

    public HistoryResponse getHistory(String ticker, int limit) {
        return execute("history for " + ticker, () -> client.get()
                .uri("/history/{t}?limit={l}", ticker, limit)
                .retrieve()
                .body(HistoryResponse.class));
    }

    public PredictResponse predict(PredictRequest request) {
        return execute("prediction for " + request.ticker(), () -> client.post()
                .uri("/predict")
                .body(request)
                .retrieve()
                .body(PredictResponse.class));
    }

    private <T> T execute(String context, Supplier<T> call) {
        for (int attempt = 1; ; attempt++) {
            try {
                return call.get();
            } catch (ResourceAccessException ex) {
                if (attempt > 1) {
                    log.error("Predictor unreachable for {} after retry: {}", context, ex.getMessage());
                    throw new UpstreamException(HttpStatus.SERVICE_UNAVAILABLE, "prediction service unavailable");
                }
                log.warn("Predictor timed out/unreachable for {} ({}), retrying once", context, ex.getMessage());
            } catch (HttpStatusCodeException ex) {
                throw translate(ex, context);
            }
        }
    }

    private ApiException translate(HttpStatusCodeException ex, String context) {
        int status = ex.getStatusCode().value();
        if (status == HttpStatus.NOT_FOUND.value()) {
            return new NotFoundException("no data available for " + context);
        }
        if (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value()) {
            // Wrong or missing X-API-Key: our misconfiguration, not the caller's fault.
            log.error("Predictor rejected our X-API-Key ({}) while fetching {}", status, context);
            return new UpstreamException(HttpStatus.BAD_GATEWAY, "prediction service misconfigured");
        }
        if (status == HttpStatus.SERVICE_UNAVAILABLE.value()) {
            log.error("Predictor reports the model is not loaded ({})", context);
            return new UpstreamException(HttpStatus.SERVICE_UNAVAILABLE, "prediction service unavailable");
        }
        log.error("Predictor returned {} for {}: {}", status, context, ex.getResponseBodyAsString());
        return new UpstreamException(HttpStatus.BAD_GATEWAY, "prediction service error (" + status + ")");
    }
}
