# Predictor Backend — API Flow Reference

What happens, step by step, for every HTTP endpoint in this service, plus the background jobs that feed them. Written so you can jump straight to the file/line doing the work instead of tracing imports by hand.

## Layers, in call order

```
HTTP request
  -> Controller   (com.stock_predictor.*.controller)   — routing, request/response DTOs only
  -> Service       (com.stock_predictor.*.service)       — business logic, transactions
  -> Repository    (com.stock_predictor.*.repository)    — Spring Data JPA, one interface per table
  -> Entity        (com.stock_predictor.*.entity)         — @Entity classes, one per table
  -> Postgres
```

Cross-cutting pieces that don't sit in that chain but touch every request:
- `common/GlobalExceptionHandler.java` — `@RestControllerAdvice`, turns thrown exceptions into JSON `{"error": "..."}` responses. This is the single place all error responses come from.
- `common/ApiError.java` — the error response shape (one field: `error`).
- `common/IndicatorJsonCodec.java` — serializes/deserializes the `features`/indicators map to/from the `TEXT` column `predictions.indicators_json`. Also normalizes a couple of legacy key names (`sma_5` -> `sma5`, etc.) via `normalizeIndicatorKey()`.
- `config/CorsConfig.java` — registers a global `CorsFilter` for `/api/**`, currently allowing only `GET`/`OPTIONS` (see the flag on the batch endpoint below).
- `config/AppProperties.java` — typed binding of `app.*` in `application.yml` (`@ConfigurationProperties`).
- `config/StockSeedData.java` — `CommandLineRunner`, runs once at startup (skipped under the `test` profile), inserts 10 hardcoded stocks into `stocks` if the table is empty.

---

## Endpoints

### 1. `GET /api/predictions/top?limit=10`
**File:** `predictions/controller/PredictionController.java:22`

1. Controller reads `limit` (query param, default 10), calls `PredictionService.getTopPredictions(limit)` (`predictions/service/PredictionService.java:36`).
2. Service clamps `limit` to `[1, 50]`.
3. Loads **all** stocks into a `ticker -> Stock` map (`StockRepository.findAll()`) — used only to look up company names.
4. Calls `PredictionRepository.findLatestPerTicker()` (`predictions/repository/PredictionRepository.java:25`) — a `@Query` that, per ticker, picks the row with the max `predicted_for_date`, ordered by `confidence DESC, created_at DESC`. This is why a ticker with no predictions yet simply doesn't appear — there's no LEFT JOIN from `stocks`.
5. Maps each `Prediction` + looked-up company name into a `TopPredictionResponse` (`predictions/dto/TopPredictionResponse.java`): `ticker, companyName, predictedTrend, confidence, predictedForDate`.
6. Returns `200` with a JSON array (empty array if no predictions exist — not a 404).

**Reads:** `predictions`, `stocks`. **Writes:** none.

---

### 2. `GET /api/predictions/accuracy?ticker=AAPL`
**File:** `predictions/controller/PredictionController.java:28`

1. Controller calls `PredictionService.getAccuracyHistory(ticker)` (`PredictionService.java:69`).
2. Service normalizes the ticker (`trim().toUpperCase()`), then `StockRepository.existsById(...)` — **throws `ResourceNotFoundException` -> `404`** if the ticker isn't in `stocks` at all.
3. `PredictionRepository.findByTickerAndActualTrendIsNotNullOrderByPredictedForDateDesc(ticker)` — only predictions that have already been graded (see the accuracy job below; ungraded/pending predictions are excluded, not shown as "unknown").
4. Computes `correct` / `total` / `accuracyPercent` in-memory, maps each row to an `AccuracyHistoryItem` (`predictions/dto/AccuracyHistoryItem.java`: `date, predicted, actual, wasCorrect`).
5. Returns `200` with `AccuracyResponse` (`predictions/dto/AccuracyResponse.java`). If the ticker exists but has zero graded predictions, this is `200` with `totalPredictions: 0`, not a 404.

**Reads:** `stocks`, `predictions`. **Writes:** none.

---

### 3. `POST /api/predictions/batch` — the ML pipeline's ingest endpoint
**File:** `predictions/controller/PredictionController.java:32` (added for the Data Contract in the root `decisions.md` Section 3a)

This is the only write endpoint and the only one with a request body, so it has the most moving parts:

1. Spring deserializes the body into `BatchPredictionRequest` (`predictions/dto/BatchPredictionRequest.java`) — a `predictions: List<PredictionBatchItem>`.
   - **If the JSON is malformed or a field has the wrong type** (e.g. `confidence` sent as a string), deserialization throws `HttpMessageNotReadableException` before the controller method even runs -> caught by `GlobalExceptionHandler.handleUnreadable()` -> `400` with a message naming the underlying parse failure.
2. Each `PredictionBatchItem` (`predictions/dto/PredictionBatchItem.java`) is Bean-Validation-checked (`@Valid` cascades into the list): `ticker` not blank, `predictionDate`/`targetDate` not null, `predictedDirection` must match `up|down`, `confidence`/`modelAccuracy` in `[0,1]`, `features` not null, `lastClosePrice` >= 0. Field names on the wire are snake_case via `@JsonProperty` (`prediction_date`, `target_date`, `predicted_direction`, `model_accuracy`, `last_close_price`) to match the Data Contract exactly, even though the Java fields are camelCase.
   - **Any validation failure** -> `MethodArgumentNotValidException` -> `GlobalExceptionHandler.handleValidation()` -> `400` listing every failing field across every record in one message (e.g. `predictions[3].features: must not be null`).
3. Controller calls `PredictionService.saveBatch(request)` (`PredictionService.java:57`), annotated `@Transactional` (overriding the class-level `@Transactional(readOnly = true)`).
4. Service maps each `PredictionBatchItem` -> `Prediction` entity via `toEntity()` (`PredictionService.java:65`):
   - `features` map is normalized and JSON-encoded via `IndicatorJsonCodec` into `indicatorsJson`.
   - `reasoning` is left `null` — this endpoint does **not** call an LLM; that happens elsewhere (or hasn't been built yet — see the architecture note below).
   - `target_date` from the contract maps to the entity's existing `predictedForDate` field/`predicted_for_date` column (no separate column was added for it).
5. `PredictionRepository.saveAll(entities)` — all rows inserted in the single transaction from step 3; if anything fails partway, the whole batch rolls back.
6. Returns `201` with `BatchPredictionResponse` (`predictions/dto/BatchPredictionResponse.java`): `{"saved": <count>}`.

**Reads:** none (does not check that `ticker` exists in `stocks` first — a batch can insert predictions for tickers `stocks` doesn't know about). **Writes:** `predictions` (insert-only, not upsert — see the note in root `decisions.md` Section 10).

**⚠ Known gap:** `CorsConfig.java` only allows `GET`/`OPTIONS` in its CORS mapping (`config/CorsConfig.java:27,39`). A browser-based client calling this `POST` cross-origin will be blocked by CORS preflight. Not an issue for the GitHub Actions Python job (server-to-server, no browser CORS enforcement), but worth fixing before any frontend ever calls this directly.

---

### 4. `GET /api/stocks/{ticker}`
**File:** `stocks/controller/StockController.java:23`

1. Controller calls `StockService.getProfile(ticker)` (`stocks/service/StockService.java:28`).
2. `StockRepository.findById(normalizedTicker)` — **`404` via `ResourceNotFoundException`** if not found.
3. Maps to `StockProfileResponse` (`stocks/dto/StockProfileResponse.java`): `ticker, companyName, sector`.

**Reads:** `stocks`. **Writes:** none.

---

### 5. `GET /api/stocks/{ticker}/prices?days=30`
**File:** `stocks/controller/StockController.java:28`

1. Controller calls `StockService.getRecentPrices(ticker, days)` (`StockService.java:34`).
2. `requireStockExists()` -> same `404` behavior as above if the ticker isn't in `stocks`.
3. `days` clamped to `[1, 365]`.
4. `StockPriceRepository.findByTickerOrderByPriceDateDesc(ticker, PageRequest.of(0, lookbackDays))` — fetches newest-first, then the service re-sorts ascending before returning (so the response is chronological, oldest -> newest).
5. Maps each `StockPrice` to a `PricePointResponse` (`stocks/dto/PricePointResponse.java`): `date, open, high, low, close, volume`.

**Reads:** `stocks`, `stock_prices`. **Writes:** none.

---

### 6. `GET /api/stocks/{ticker}/prediction`
**File:** `stocks/controller/StockPredictionController.java:20`

Lives in the `stocks` package/URL space but delegates entirely to the `predictions` service:
1. Controller calls `PredictionService.getLatestPrediction(ticker)` (`PredictionService.java:56`).
2. `404` if the ticker isn't in `stocks`; **separately**, `404` if the ticker exists but has no prediction row yet (`PredictionRepository.findTopByTickerOrderByPredictedForDateDescCreatedAtDesc`).
3. Maps to `PredictionDetailResponse` (`predictions/dto/PredictionDetailResponse.java`): `ticker, trend, confidence, reasoning, indicators` — `indicators` is the *decoded* `features`/indicators map (via `IndicatorJsonCodec.indicatorsFromJson()`), not the raw JSON string.

**Reads:** `stocks`, `predictions`. **Writes:** none.

---

## Background jobs (not HTTP endpoints, but they're the other way data enters/changes)

All three are `@Scheduled` `@Component`s in `ingestion/job/`, each just calling one `IngestionService` method — disabled entirely under the `test` profile (`spring.task.scheduling.enabled: false`) and given `cron: "-"` (never fires) in test config.

| Job | File | Cron property | Calls |
|---|---|---|---|
| `DataIngestionJob` | `ingestion/job/DataIngestionJob.java` | `app.ingestion.cron` (default `0 0 18 * * *`) | `IngestionService.ingestPricesForAllStocks()` — pulls OHLCV from FMP (`ingestion/fmp/FmpClient.java`) for every stock, skips dates already present |
| `PredictionJob` | `ingestion/job/PredictionJob.java` | `app.prediction.cron` (default `0 30 18 * * *`) | `IngestionService.generatePredictionsForAllStocks()` — see architecture note below |
| `AccuracyCheckJob` | `ingestion/job/AccuracyCheckJob.java` | `app.accuracy.cron` (default `0 0 19 * * *`) | `IngestionService.evaluatePendingPredictions()` — for any prediction whose `predicted_for_date` has passed and isn't graded yet, compares the actual close price on that date vs. the prior day (`IngestionService.classifyTrend()`, `> 0.1%` move = up/down, else `flat`), sets `actual_trend`/`was_correct` |

### ⚠ Architecture note worth flagging to the team

`PredictionJob` / `IngestionService.generatePredictionForTicker()` (`ingestion/service/IngestionService.java:100`) calls `MlServiceClient.predict()` (`ingestion/ml/MlServiceClient.java`) **live, synchronously, from the backend**, to a configurable `ML_SERVICE_URL`. This is a *second, separate* code path into the `predictions` table alongside the new `POST /api/predictions/batch` endpoint (#3 above) — and it's the exact pattern the root `decisions.md` (Section 9, "Explicitly Rejected/Reconsidered Approaches") says was **rejected** in favor of the push-based batch model: *"Backend calling a live Python API ... rejected in favor of a push model (ML layer sends data outward)."*

Both paths write to the same `predictions` table with mostly-compatible shapes (this session extended the `Prediction` entity so both constructors still work), so nothing is currently broken by having both — but if `PredictionJob`'s cron is ever enabled in a real environment where `ML_SERVICE_URL` points at something live, you'd get predictions arriving through two independent channels with different metadata (the live path doesn't set `prediction_date`, `model_accuracy`, or `last_close_price` — those stay `null`). Worth a decision with the team: is `PredictionJob`/`MlServiceClient`/`FmpClient`'s live-prediction path dead code to remove, or a real fallback path to keep documented as intentional?

---

## Where things are, at a glance

```
src/main/java/com/stock_predictor/
├── common/            ApiError, GlobalExceptionHandler, IndicatorJsonCodec, ResourceNotFoundException
├── config/            AppProperties, CorsConfig, RestClientConfig, StockSeedData
├── ingestion/
│   ├── fmp/           FmpClient, FmpPriceRecord           (external price data source)
│   ├── ml/             MlServiceClient, MlPredictRequest/Response  (live-call path, see note above)
│   ├── job/            DataIngestionJob, PredictionJob, AccuracyCheckJob   (@Scheduled)
│   └── service/        IngestionService                   (orchestrates all three jobs)
├── predictions/
│   ├── controller/     PredictionController                (/api/predictions/*)
│   ├── service/        PredictionService
│   ├── repository/     PredictionRepository
│   ├── entity/         Prediction
│   └── dto/             BatchPredictionRequest, PredictionBatchItem, BatchPredictionResponse,
│                        TopPredictionResponse, PredictionDetailResponse, AccuracyResponse, AccuracyHistoryItem
└── stocks/
    ├── controller/      StockController, StockPredictionController   (/api/stocks/*)
    ├── service/         StockService
    ├── repository/      StockRepository, StockPriceRepository
    ├── entity/          Stock, StockPrice
    └── dto/              StockProfileResponse, PricePointResponse
```

Tests: `src/test/java/.../predictions/controller/PredictionControllerIntegrationTest.java` (H2, in-memory, endpoints #1/#4/#6) and `PredictionBatchIntegrationTest.java` (real local Postgres, endpoint #3 — 5 cases covering a valid 50-record batch plus four malformed-payload cases).
