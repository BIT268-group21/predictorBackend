package com.tradingapp.prediction;

import com.tradingapp.prediction.dto.PredictionHistoryItem;
import com.tradingapp.prediction.dto.PredictionResult;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PredictionController {

    private final PredictionService predictionService;

    public PredictionController(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    /**
     * Public, but optionally authenticated: when a valid JWT is present the
     * prediction is also written to that user's history.
     */
    @GetMapping("/predict/{ticker}")
    public PredictionResult predict(@PathVariable String ticker,
                                    @AuthenticationPrincipal UserDetails principal) {
        String email = principal == null ? null : principal.getUsername();
        return predictionService.predict(ticker, email);
    }

    @GetMapping("/predictions/history")
    public List<PredictionHistoryItem> history(@AuthenticationPrincipal UserDetails principal) {
        return predictionService.historyForUser(principal.getUsername());
    }
}
