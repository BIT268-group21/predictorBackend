package com.stock_predictor.ingestion.twelvedata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.stock_predictor.config.AppProperties;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TwelveDataClientTest {

	private static final String BASE_URL = "http://fake-twelvedata";
	private static final String URI_TEMPLATE =
			BASE_URL + "/time_series?symbol={symbol}&interval=1day&outputsize={outputsize}&apikey={apikey}";

	private TwelveDataClient client(MockRestServiceServer[] serverOut, String apiKey) {
		RestClient.Builder builder = RestClient.builder();
		serverOut[0] = MockRestServiceServer.bindTo(builder).build();
		RestClient restClient = builder.build();
		AppProperties appProperties = new AppProperties(
				new AppProperties.TwelveDataProperties(apiKey, BASE_URL), null, null, null, null);
		return new TwelveDataClient(restClient, appProperties);
	}

	@Test
	void successfulFetchParsesAndSortsAscending() {
		MockRestServiceServer[] serverHolder = new MockRestServiceServer[1];
		TwelveDataClient client = client(serverHolder, "test-key");

		String body = """
				{
				  "meta": {"symbol": "AAPL"},
				  "values": [
				    {"datetime": "2026-08-06", "open": "227.5", "high": "229.1", "low": "226.0", "close": "228.4", "volume": "45000000"},
				    {"datetime": "2026-08-05", "open": "225.0", "high": "228.0", "low": "224.5", "close": "227.0", "volume": "40000000"}
				  ],
				  "status": "ok"
				}
				""";

		serverHolder[0].expect(requestToUriTemplate(URI_TEMPLATE, "AAPL", 10, "test-key"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

		List<TwelveDataPriceRecord> records = client.fetchHistoricalPrices("AAPL");

		assertEquals(2, records.size());
		assertEquals("2026-08-05", records.get(0).date());
		assertEquals("2026-08-06", records.get(1).date());
		assertEquals(new BigDecimal("225.0"), records.get(0).open());
		assertEquals(new BigDecimal("228.4"), records.get(1).close());
		assertEquals(40_000_000L, records.get(0).volume());
		serverHolder[0].verify();
	}

	@Test
	void blankApiKeySkipsGracefullyWithoutCallingTwelveData() {
		MockRestServiceServer[] serverHolder = new MockRestServiceServer[1];
		TwelveDataClient client = client(serverHolder, "");

		List<TwelveDataPriceRecord> records = client.fetchHistoricalPrices("AAPL");

		assertTrue(records.isEmpty());
		serverHolder[0].verify();
	}

	@Test
	void errorStatusIsHandledWithoutThrowing() {
		MockRestServiceServer[] serverHolder = new MockRestServiceServer[1];
		TwelveDataClient client = client(serverHolder, "test-key");

		String body = """
				{"status": "error", "message": "**symbol** not found: INVALID"}
				""";

		serverHolder[0].expect(requestToUriTemplate(URI_TEMPLATE, "INVALID", 10, "test-key"))
				.andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

		List<TwelveDataPriceRecord> records = client.fetchHistoricalPrices("INVALID");

		assertTrue(records.isEmpty());
		serverHolder[0].verify();
	}

	@Test
	void unparsableValueIsSkippedRatherThanFailingTheWholeFetch() {
		MockRestServiceServer[] serverHolder = new MockRestServiceServer[1];
		TwelveDataClient client = client(serverHolder, "test-key");

		String body = """
				{
				  "meta": {"symbol": "AAPL"},
				  "values": [
				    {"datetime": "2026-08-06", "open": "not-a-number", "high": "229.1", "low": "226.0", "close": "228.4", "volume": "45000000"},
				    {"datetime": "2026-08-05", "open": "225.0", "high": "228.0", "low": "224.5", "close": "227.0", "volume": "40000000"}
				  ],
				  "status": "ok"
				}
				""";

		serverHolder[0].expect(requestToUriTemplate(URI_TEMPLATE, "AAPL", 10, "test-key"))
				.andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

		List<TwelveDataPriceRecord> records = client.fetchHistoricalPrices("AAPL");

		assertEquals(1, records.size());
		assertEquals("2026-08-05", records.get(0).date());
	}
}
