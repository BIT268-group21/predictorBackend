package com.stock_predictor.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Shared-secret bearer-token check for the machine-to-machine batch ingest
 * endpoint (decisions.md Section 2a/15) — the nightly ML job authenticates
 * with {@code Authorization: Bearer <BATCH_AUTH_TOKEN>}. Every other endpoint
 * (the GET reads the frontend uses) is left untouched.
 *
 * Fails closed: if {@code app.batch.auth-token} isn't configured, every
 * request to the batch endpoint is rejected rather than let through.
 */
@Component
public class BatchAuthInterceptor implements HandlerInterceptor {

	private static final String BATCH_ENDPOINT_PATH = "/api/predictions/batch";
	private static final String BEARER_PREFIX = "Bearer ";

	private final AppProperties appProperties;

	public BatchAuthInterceptor(AppProperties appProperties) {
		this.appProperties = appProperties;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (!"POST".equals(request.getMethod()) || !BATCH_ENDPOINT_PATH.equals(request.getRequestURI())) {
			return true;
		}

		String configuredToken = appProperties.batch() != null ? appProperties.batch().authToken() : null;
		String authorizationHeader = request.getHeader("Authorization");
		String expectedHeader = configuredToken != null ? BEARER_PREFIX + configuredToken : null;

		boolean tokenConfigured = configuredToken != null && !configuredToken.isBlank();
		boolean headerMatches = expectedHeader != null && expectedHeader.equals(authorizationHeader);

		if (!tokenConfigured || !headerMatches) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return false;
		}

		return true;
	}
}
