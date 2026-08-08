package com.stock_predictor.ingestion.twelvedata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;

/**
 * A single day's OHLCV bar, already parsed into usable types. This is the
 * shape {@link TwelveDataClient#fetchHistoricalPrices(String)} returns and
 * {@code IngestionService} consumes.
 */
public record TwelveDataPriceRecord(
		String date,
		BigDecimal open,
		BigDecimal high,
		BigDecimal low,
		BigDecimal close,
		Long volume) {

	/**
	 * The raw Twelve Data {@code GET /time_series} response:
	 * {@code {"meta": {...}, "values": [{"datetime": "...", "open": "...", ...}], "status": "ok"}}.
	 * On error, {@code status} is {@code "error"} and {@code message} is set instead of {@code values}.
	 * Ignores unknown properties since Twelve Data's {@code meta} object carries more fields
	 * (currency, exchange, mic_code, ...) than this client needs.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record TimeSeriesResponse(Meta meta, List<RawValue> values, String status, String message) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Meta(String symbol) {
	}

	/** One entry of {@code values}, exactly as Twelve Data sends it: every field a JSON string. */
	public record RawValue(String datetime, String open, String high, String low, String close, String volume) {
	}
}
