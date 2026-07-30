package com.tradingapp.common;

import java.time.Instant;

/** Consistent error body returned for every failed request. */
public record ApiError(Instant timestamp, int status, String error, String message, String path) {
}
