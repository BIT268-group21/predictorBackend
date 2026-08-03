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
- `config/CorsConfig.java` — registers a global `CorsFilter` for `/api/**`, currently allowing only `GET`/`OPTIONS` (see the flag on the batch endpoint below).
- `config/AppProperties.java` — typed binding of `app.*` in `application.yml` (`@ConfigurationProperties`).
- `config/StockSeedData.java` — `CommandLineRunner`, runs once at startup (skipped under the `test` profile), inserts 50 hardcoded stocks into `stocks` if the table is empty (mirrors the ML pipeline's 50-ticker universe).

---

## Schema (normalized to 3NF, last revised 2026-07-31)

All tables have a single-column key (`ticker` for `stocks`; a surrogate `id` for `stock_prices`/`predictions`/`prediction_features`), so 2NF (no partial dependency on part of a composite key) is trivially satisfied everywhere except `prediction_features`, addressed below. What actually needed fixing was around `predictions`' feature data:

- **1NF fix, first pass (2026-07-30):** the four features from the *original* Data Contract example (`moving_avg_short`, `moving_avg_long`, `volatility_20d`, `momentum_5d`) used to be stored together as one JSON string in a single `indicators_json TEXT` column — not an atomic value. Replaced with four separate `NUMERIC` columns directly on `predictions`.
- **Superseded, 2026-07-31:** that fixed-four-column design conflicted with the ML pipeline's real behavior — it does per-stock correlation-based feature selection (decisions.md Section 7/14), so the actual feature *names* and *count* differ per stock (e.g. AAPL might get 5 features, JPM a different 3, no overlap). Fixed columns can't represent that without either losing data or hard-coding a superset. **Current design:** a child table, `prediction_features` (entity `PredictionFeature`) — one row per `(prediction, feature_name)`:
  - `id` (surrogate PK), `prediction_id` (FK to `predictions.id`), `feature_name` (varchar), `feature_value` (numeric) — plus a unique constraint on `(prediction_id, feature_name)`, the real candidate key.
  - **1NF:** every column atomic (one name, one value per row — no blob, no repeating group).
  - **2NF:** the natural key is the composite `(prediction_id, feature_name)`; `feature_value` depends on *both* parts together, not on either alone — no partial dependency.
  - **3NF:** nothing else non-key to be transitively dependent on anything.
  - This is the standard relational pattern for a variable-arity attribute set (sometimes called an EAV/name-value child table) — it's what lets the schema hold *any* per-stock feature set without a migration every time the ML pipeline's feature selection changes.
- **3NF fix (`was_correct`, 2026-07-30, unchanged since):** used to be a stored `BOOLEAN` column on `predictions`, but its value is always exactly `actual_trend.equals(predicted_trend)` — a transitive dependency on two other non-key columns, not an independent fact. It's a computed method (`Prediction.getWasCorrect()`), not a persisted column; `AccuracyCheckJob` only sets `actual_trend`, and correctness is derived on read.

`stocks` (`ticker` PK, `company_name`, `sector`) and `stock_prices` (surrogate `id` PK, unique on `ticker`+`price_date`, plain OHLCV columns) were already in 3NF — no changes needed there.

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
2. Each `PredictionBatchItem` (`predictions/dto/PredictionBatchItem.java`) is Bean-Validation-checked (`@Valid` cascades into the list): `ticker` not blank, `predictionDate`/`targetDate` not null, `predictedDirection` must match `up|down`, `confidence`/`modelAccuracy` in `[0,1]`, `features` a non-empty `Map<String, BigDecimal>` (`@NotEmpty` — deliberately *not* a fixed set of named fields, since the ML pipeline's per-stock feature selection means the keys and count genuinely vary per stock; see the Schema section above), `lastClosePrice` >= 0. Field names on the wire are snake_case via `@JsonProperty` (`prediction_date`, `target_date`, `predicted_direction`, `model_accuracy`, `last_close_price`) to match the Data Contract exactly, even though the Java fields are camelCase — `features`' own keys are passed through as-is (they're map keys, not annotated Java fields).
   - **Any validation failure** -> `MethodArgumentNotValidException` -> `GlobalExceptionHandler.handleValidation()` -> `400` listing every failing field across every record in one message (e.g. `predictions[3].ticker: must not be blank`).
3. Controller calls `PredictionService.saveBatch(request)` (`PredictionService.java:58`), annotated `@Transactional` (overriding the class-level `@Transactional(readOnly = true)`).
4. Service maps each `PredictionBatchItem` -> `Prediction` entity via `toEntity()`, then `PredictionRepository.saveAll(entities)` — this populates each entity's generated `id` (needed for step 5). `reasoning` is left `null` on the entity — this endpoint does **not** call an LLM, and the reasoning field is not currently produced. `target_date` from the contract maps to the entity's existing `predictedForDate` field/`predicted_for_date` column.
5. For every `(name, value)` pair in each item's `features` map, builds a `PredictionFeature(savedPrediction, name, value)` and `PredictionFeatureRepository.saveAll(...)`s them all — still inside the same transaction from step 3, so a batch either fully commits (predictions + all their features) or fully rolls back.
6. Returns `201` with `BatchPredictionResponse` (`predictions/dto/BatchPredictionResponse.java`): `{"saved": <count>}` (count of predictions, not feature rows).

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
3. Maps to `PredictionDetailResponse` (`predictions/dto/PredictionDetailResponse.java`): `ticker, trend, confidence, reasoning, indicators` — `indicators` is built by `PredictionService.toIndicatorsMap()`, which queries `PredictionFeatureRepository.findByPredictionId(...)` and turns the rows back into a `name -> value` map. Whatever feature names/count that particular prediction was stored with is exactly what comes back here — nothing fixed or assumed.

**Reads:** `stocks`, `predictions`. **Writes:** none.

---

## Background jobs (not HTTP endpoints, but they're the other way data enters/changes)

The **only** writer to the `predictions` table is `POST /api/predictions/batch` (#3 above): the ML service is the sole producer of predictions, pushing them in via that endpoint. The backend never generates a prediction itself — it only grades predictions once their `predicted_for_date` has passed (see `AccuracyCheckJob` below).

The remaining `@Scheduled` `@Component`s in `ingestion/job/` each call one `IngestionService` method — disabled entirely under the `test` profile (`spring.task.scheduling.enabled: false`) and given `cron: "-"` (never fires) in test config.

| Job | File | Cron property | Calls |
|---|---|---|---|
| `DataIngestionJob` | `ingestion/job/DataIngestionJob.java` | `app.ingestion.cron` (default `0 0 18 * * *`) | `IngestionService.ingestPricesForAllStocks()` — pulls OHLCV from FMP (`ingestion/fmp/FmpClient.java`) for every stock, skips dates already present |
| `AccuracyCheckJob` | `ingestion/job/AccuracyCheckJob.java` | `app.accuracy.cron` (default `0 0 19 * * *`) | `IngestionService.evaluatePendingPredictions()` — for any prediction whose `predicted_for_date` has passed and isn't graded yet, compares the actual close price on that date vs. the prior day (`IngestionService.classifyTrend()`, `> 0.1%` move = up/down, else `flat`), sets `actual_trend` only — `was_correct` is no longer a stored column (3NF fix, see Schema section above); it's derived from `actual_trend`/`predicted_trend` at read time |

---

## Where things are, at a glance

```
src/main/java/com/stock_predictor/
├── common/            ApiError, GlobalExceptionHandler, ResourceNotFoundException
├── config/            AppProperties, CorsConfig, RestClientConfig, StockSeedData
├── ingestion/
│   ├── fmp/           FmpClient, FmpPriceRecord           (external price data source)
│   ├── job/            DataIngestionJob, AccuracyCheckJob   (@Scheduled)
│   └── service/        IngestionService                   (orchestrates the scheduled jobs)
├── predictions/
│   ├── controller/     PredictionController                (/api/predictions/*)
│   ├── service/        PredictionService
│   ├── repository/     PredictionRepository, PredictionFeatureRepository
│   ├── entity/         Prediction, PredictionFeature        (feature name/value child table)
│   └── dto/             BatchPredictionRequest, PredictionBatchItem, BatchPredictionResponse,
│                        TopPredictionResponse, PredictionDetailResponse, AccuracyResponse, AccuracyHistoryItem
└── stocks/
    ├── controller/      StockController, StockPredictionController   (/api/stocks/*)
    ├── service/         StockService
    ├── repository/      StockRepository, StockPriceRepository
    ├── entity/          Stock, StockPrice
    └── dto/              StockProfileResponse, PricePointResponse
```

Tests: `src/test/java/.../predictions/controller/PredictionControllerIntegrationTest.java` (H2, in-memory, endpoints #1/#4/#6) and `PredictionBatchIntegrationTest.java` (real local Postgres, endpoint #3 — 6 cases: a valid 50-record batch, four malformed-payload cases, and one proving two different tickers can have completely different feature names/counts in the same batch).
