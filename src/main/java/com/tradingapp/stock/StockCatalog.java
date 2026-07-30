package com.tradingapp.stock;

import java.util.List;
import org.springframework.stereotype.Component;

/** Curated, static list of tickers the frontend offers for browsing. */
@Component
public class StockCatalog {

    public record StockItem(String ticker, String name) {
    }

    private static final List<StockItem> STOCKS = List.of(
            new StockItem("AAPL", "Apple Inc."),
            new StockItem("MSFT", "Microsoft Corporation"),
            new StockItem("GOOGL", "Alphabet Inc."),
            new StockItem("AMZN", "Amazon.com, Inc."),
            new StockItem("META", "Meta Platforms, Inc."),
            new StockItem("NVDA", "NVIDIA Corporation"),
            new StockItem("TSLA", "Tesla, Inc."),
            new StockItem("NFLX", "Netflix, Inc."),
            new StockItem("AMD", "Advanced Micro Devices, Inc."),
            new StockItem("INTC", "Intel Corporation"),
            new StockItem("JPM", "JPMorgan Chase & Co."),
            new StockItem("BAC", "Bank of America Corporation"),
            new StockItem("V", "Visa Inc."),
            new StockItem("MA", "Mastercard Incorporated"),
            new StockItem("DIS", "The Walt Disney Company"),
            new StockItem("KO", "The Coca-Cola Company"),
            new StockItem("PEP", "PepsiCo, Inc."),
            new StockItem("WMT", "Walmart Inc."),
            new StockItem("XOM", "Exxon Mobil Corporation"),
            new StockItem("JNJ", "Johnson & Johnson"),
            new StockItem("PFE", "Pfizer Inc."),
            new StockItem("BA", "The Boeing Company"),
            new StockItem("UBER", "Uber Technologies, Inc."),
            new StockItem("SHOP", "Shopify Inc."),
            new StockItem("SPY", "SPDR S&P 500 ETF Trust"));

    public List<StockItem> all() {
        return STOCKS;
    }
}
