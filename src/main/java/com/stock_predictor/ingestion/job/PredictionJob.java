package com.stock_predictor.ingestion.job;

import com.stock_predictor.ingestion.service.IngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PredictionJob {

	private static final Logger log = LoggerFactory.getLogger(PredictionJob.class);

	private final IngestionService ingestionService;

	public PredictionJob(IngestionService ingestionService) {
		this.ingestionService = ingestionService;
	}

	@Scheduled(cron = "${app.prediction.cron:0 30 18 * * *}")
	public void run() {
		log.info("Starting scheduled prediction generation");
		ingestionService.generatePredictionsForAllStocks();
	}
}
