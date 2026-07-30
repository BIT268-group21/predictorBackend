package com.tradingapp.watchlist.dto;

import com.tradingapp.watchlist.WatchlistItem;
import java.time.Instant;

public record WatchlistResponse(Long id, String ticker, Instant createdAt) {

    public static WatchlistResponse from(WatchlistItem item) {
        return new WatchlistResponse(item.getId(), item.getTicker(), item.getCreatedAt());
    }
}
