package com.tradingapp.common;

import org.springframework.http.HttpStatus;

/**
 * Raised when the Python prediction microservice fails or rejects our input.
 * The status is the one the frontend should see (422 bad input, 502
 * misconfiguration, 503 predictor unavailable) — see BUILD_SPEC §3.
 */
public class UpstreamException extends ApiException {

    public UpstreamException(HttpStatus status, String message) {
        super(status, message);
    }
}
