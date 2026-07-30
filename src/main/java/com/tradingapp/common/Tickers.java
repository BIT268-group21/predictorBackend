package com.tradingapp.common;

/** Tickers are uppercased and trimmed before they are stored or forwarded. */
public final class Tickers {

    private Tickers() {
    }

    public static String normalize(String rawTicker) {
        if (rawTicker == null || rawTicker.isBlank()) {
            throw new BadRequestException("ticker is required");
        }
        return rawTicker.trim().toUpperCase();
    }
}
