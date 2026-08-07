package com.stock_predictor.config;

import java.util.Arrays;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class CorsConfig implements WebMvcConfigurer {

	private final AppProperties appProperties;
	private final BatchAuthInterceptor batchAuthInterceptor;

	public CorsConfig(AppProperties appProperties, BatchAuthInterceptor batchAuthInterceptor) {
		this.appProperties = appProperties;
		this.batchAuthInterceptor = batchAuthInterceptor;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
				.allowedOrigins(allowedOrigins())
				.allowedMethods("GET", "OPTIONS")
				.allowedHeaders("*");
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(batchAuthInterceptor);
	}

	@Bean
	CorsFilter corsFilter() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowCredentials(false);
		for (String origin : allowedOrigins()) {
			config.addAllowedOrigin(origin);
		}
		config.addAllowedHeader("*");
		config.addAllowedMethod("GET");
		config.addAllowedMethod("OPTIONS");

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", config);
		return new CorsFilter(source);
	}

	private String[] allowedOrigins() {
		return Arrays.stream(appProperties.cors().allowedOrigins().split(","))
				.map(String::trim)
				.filter(origin -> !origin.isEmpty())
				.toArray(String[]::new);
	}
}
