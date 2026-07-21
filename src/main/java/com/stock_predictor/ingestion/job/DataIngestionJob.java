package com.stock_predictor.ingestion.job;

import com.stock_predictor.ingestion.service.IngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DataIngestionJob {

	private static final Logger log = LoggerFactory.getLogger(DataIngestionJob.class);

	private final IngestionService ingestionService;

	public DataIngestionJob(IngestionService ingestionService) {
		this.ingestionService = ingestionService;
	}

	@Scheduled(cron = "${app.ingestion.cron:0 0 18 * * *}")
	public void run() {
		log.info("Starting scheduled price ingestion");
		ingestionService.ingestPricesForAllStocks();
	}
}
