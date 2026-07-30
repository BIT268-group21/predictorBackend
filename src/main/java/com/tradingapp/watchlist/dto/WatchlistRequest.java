package com.tradingapp.watchlist.dto;

import jakarta.validation.constraints.NotBlank;

public record WatchlistRequest(@NotBlank String ticker) {
}
