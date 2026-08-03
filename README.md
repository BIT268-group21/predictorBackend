# Group 21 predictor backend

## Run with Docker

Build and start the backend plus its Postgres database:

```
docker compose up --build
```

The API is then available at `http://localhost:8080`.

### Configuration

| Env var | Default | Purpose |
|---|---|---|
| `FMP_API_KEY` | *(empty)* | Financial Modeling Prep API key used by the scheduled ingestion job |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Comma-separated list of allowed frontend origins |

Set any of these by exporting them in the shell before `docker compose up`, or via a `.env` file next to `docker-compose.yml`.

### Build notes

- The image build skips tests (`-DskipTests`); `PredictionBatchIntegrationTest` needs a live Postgres and runs against your local/dev setup, not inside the image build.
- Postgres data persists in the `postgres_data` named volume.

### Local (non-Docker) run

```
./mvnw spring-boot:run
```
