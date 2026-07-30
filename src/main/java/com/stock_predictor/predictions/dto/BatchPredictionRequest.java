package com.stock_predictor.predictions.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BatchPredictionRequest(@NotEmpty @Valid List<PredictionBatchItem> predictions) {
}
