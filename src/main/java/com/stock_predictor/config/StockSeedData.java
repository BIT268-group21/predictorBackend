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
		stockRepository.saveAll(List.of(
				new Stock("AAPL", "Apple Inc.", "Technology"),
				new Stock("MSFT", "Microsoft Corporation", "Technology"),
				new Stock("GOOGL", "Alphabet Inc.", "Technology"),
				new Stock("AMZN", "Amazon.com Inc.", "Consumer Cyclical"),
				new Stock("NVDA", "NVIDIA Corporation", "Technology"),
				new Stock("META", "Meta Platforms Inc.", "Technology"),
				new Stock("TSLA", "Tesla Inc.", "Consumer Cyclical"),
				new Stock("JPM", "JPMorgan Chase & Co.", "Financial Services"),
				new Stock("V", "Visa Inc.", "Financial Services"),
				new Stock("JNJ", "Johnson & Johnson", "Healthcare")));
	}
}
