package com.stock_predictor.config;

import com.stock_predictor.stocks.entity.Stock;
import com.stock_predictor.stocks.repository.StockRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class StockSeedData implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(StockSeedData.class);

	private final StockRepository stockRepository;

	public StockSeedData(StockRepository stockRepository) {
		this.stockRepository = stockRepository;
	}

	@Override
	public void run(String... args) {
		if (stockRepository.count() > 0) {
			return;
		}

		log.info("Seeding tracked stocks");
		// Mirrors the ML pipeline's 50-ticker universe (predictor_ML_model, scripts/
		// pull_and_check.py / run_nightly_batch.py), sector labels per its sectors.py.
		stockRepository.saveAll(List.of(
				new Stock("AAPL", "Apple Inc.", "Technology"),
				new Stock("MSFT", "Microsoft Corporation", "Technology"),
				new Stock("GOOGL", "Alphabet Inc.", "Technology"),
				new Stock("AMZN", "Amazon.com Inc.", "Consumer Discretionary"),
				new Stock("META", "Meta Platforms Inc.", "Communication Services"),
				new Stock("NVDA", "NVIDIA Corporation", "Technology"),
				new Stock("TSLA", "Tesla Inc.", "Consumer Discretionary"),
				new Stock("BRK.B", "Berkshire Hathaway Inc.", "Financials"),
				new Stock("JPM", "JPMorgan Chase & Co.", "Financials"),
				new Stock("V", "Visa Inc.", "Financials"),
				new Stock("JNJ", "Johnson & Johnson", "Healthcare"),
				new Stock("WMT", "Walmart Inc.", "Consumer Staples"),
				new Stock("PG", "Procter & Gamble Co.", "Consumer Staples"),
				new Stock("MA", "Mastercard Inc.", "Financials"),
				new Stock("UNH", "UnitedHealth Group Inc.", "Healthcare"),
				new Stock("HD", "Home Depot Inc.", "Consumer Discretionary"),
				new Stock("DIS", "Walt Disney Co.", "Communication Services"),
				new Stock("BAC", "Bank of America Corp.", "Financials"),
				new Stock("XOM", "Exxon Mobil Corp.", "Energy"),
				new Stock("CVX", "Chevron Corp.", "Energy"),
				new Stock("KO", "Coca-Cola Co.", "Consumer Staples"),
				new Stock("PEP", "PepsiCo Inc.", "Consumer Staples"),
				new Stock("ABBV", "AbbVie Inc.", "Healthcare"),
				new Stock("COST", "Costco Wholesale Corp.", "Consumer Staples"),
				new Stock("MRK", "Merck & Co. Inc.", "Healthcare"),
				new Stock("ADBE", "Adobe Inc.", "Technology"),
				new Stock("CRM", "Salesforce Inc.", "Technology"),
				new Stock("NFLX", "Netflix Inc.", "Communication Services"),
				new Stock("INTC", "Intel Corp.", "Technology"),
				new Stock("AMD", "Advanced Micro Devices Inc.", "Technology"),
				new Stock("CSCO", "Cisco Systems Inc.", "Technology"),
				new Stock("PFE", "Pfizer Inc.", "Healthcare"),
				new Stock("TMO", "Thermo Fisher Scientific Inc.", "Healthcare"),
				new Stock("ABT", "Abbott Laboratories", "Healthcare"),
				new Stock("NKE", "Nike Inc.", "Consumer Discretionary"),
				new Stock("MCD", "McDonald's Corp.", "Consumer Discretionary"),
				new Stock("ORCL", "Oracle Corp.", "Technology"),
				new Stock("IBM", "International Business Machines Corp.", "Technology"),
				new Stock("QCOM", "Qualcomm Inc.", "Technology"),
				new Stock("TXN", "Texas Instruments Inc.", "Technology"),
				new Stock("HON", "Honeywell International Inc.", "Industrials"),
				new Stock("UPS", "United Parcel Service Inc.", "Industrials"),
				new Stock("CAT", "Caterpillar Inc.", "Industrials"),
				new Stock("GS", "Goldman Sachs Group Inc.", "Financials"),
				new Stock("MS", "Morgan Stanley", "Financials"),
				new Stock("BA", "Boeing Co.", "Industrials"),
				new Stock("GE", "GE Aerospace", "Industrials"),
				new Stock("LMT", "Lockheed Martin Corp.", "Industrials"),
				new Stock("SBUX", "Starbucks Corp.", "Consumer Discretionary"),
				new Stock("LOW", "Lowe's Companies Inc.", "Consumer Discretionary")));
	}
}
