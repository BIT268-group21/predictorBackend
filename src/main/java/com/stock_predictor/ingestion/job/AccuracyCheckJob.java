package com.stock_predictor.ingestion.job;

import com.stock_predictor.ingestion.service.IngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AccuracyCheckJob {

	private static final Logger log = LoggerFactory.getLogger(AccuracyCheckJob.class);

	private final IngestionService ingestionService;

	public AccuracyCheckJob(IngestionService ingestionService) {
		this.ingestionService = ingestionService;
	}

	@Scheduled(cron = "${app.accuracy.cron:0 0 19 * * *}")
	public void run() {
		log.info("Starting scheduled accuracy evaluation");
		ingestionService.evaluatePendingPredictions();
	}
}
