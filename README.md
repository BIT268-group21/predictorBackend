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
| `TWELVE_DATA_API_KEY` | *(empty)* | Twelve Data API key used by the scheduled ingestion job (same key/value as the ML pipeline's `TWELVE_DATA_API_KEY`, so one external data source serves the whole system) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Comma-separated list of allowed frontend origins |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/stock_predictor?schema=public&user=postgres&password=password` | Full JDBC connection string, including credentials as query params (see `application.yml` comment for why). Override entirely in production — don't just override user/password. |
| `BATCH_AUTH_TOKEN` | *(empty — endpoint rejects all requests until this is set)* | Shared secret the ML batch job must send as `Authorization: Bearer <token>` on `POST /api/predictions/batch`. Missing or mismatched token returns `401`. |

Set any of these by exporting them in the shell before `docker compose up`, or via a `.env` file next to `docker-compose.yml`.

### Build notes

- The image build skips tests (`-DskipTests`); `PredictionBatchIntegrationTest` needs a live Postgres and runs against your local/dev setup, not inside the image build.
- Postgres data persists in the `postgres_data` named volume.

### Local (non-Docker) run

```
./mvnw spring-boot:run
```

## Production deployment (AWS)

This backend runs on **AWS App Runner** in production, backed by **RDS for PostgreSQL**, reached over a **VPC Connector** (not publicly accessible).

`DATABASE_URL`, `BATCH_AUTH_TOKEN`, `CORS_ALLOWED_ORIGINS`, and `TWELVE_DATA_API_KEY` are injected as App Runner environment variables sourced from **AWS Systems Manager Parameter Store** — they are not set in this repo, not in `docker-compose.yml`, and not in any committed file. `TWELVE_DATA_API_KEY` reuses the same Parameter Store value the ML pipeline already reads (`/ml-batch/TWELVE_DATA_API_KEY`), rather than provisioning a second one.

AWS deployment (App Runner, RDS, Parameter Store, IAM) is provisioned and owned solely by the project's single deployer (see the project's `decisions.md` Section 2 in the `KojoGyan/Group21-GSE-StockPredictor` repo, if you want the full reasoning). Contributors to this repo do not need, and are not given, AWS console or CLI access — pushing code to this repo (via a PR) is the full extent of what's needed to ship a change to production; a separate deploy step (outside this repo) picks it up from there.
