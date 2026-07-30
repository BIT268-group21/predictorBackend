package com.tradingapp.stock;

import com.tradingapp.prediction.PredictionService;
import com.tradingapp.prediction.dto.HistoryResponse;
import com.tradingapp.stock.StockCatalog.StockItem;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public browsing endpoints — no account required. */
@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final StockCatalog stockCatalog;
    private final PredictionService predictionService;

    public StockController(StockCatalog stockCatalog, PredictionService predictionService) {
        this.stockCatalog = stockCatalog;
        this.predictionService = predictionService;
    }

    @GetMapping
    public List<StockItem> stocks() {
        return stockCatalog.all();
    }

    @GetMapping("/{ticker}/history")
    public HistoryResponse history(@PathVariable String ticker,
                                   @RequestParam(defaultValue = "60") int limit) {
        return predictionService.history(ticker, limit);
    }
}
