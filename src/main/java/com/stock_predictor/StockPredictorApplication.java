package com.stock_predictor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StockPredictorApplication {

	public static void main(String[] args) {
		SpringApplication.run(StockPredictorApplication.class, args);
	}
}
