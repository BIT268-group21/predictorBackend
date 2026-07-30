# Trading backend (Group 21)

Spring Boot backend for the stock-trend trading app, built to [BUILD_SPEC.md](BUILD_SPEC.md).
It owns users, auth, watchlists, prediction history and price alerts, and orchestrates
predictions by calling the Python prediction microservice (see BUILD_SPEC §3).

- Java 17 · Spring Boot 3.3.5 · Maven
- Spring Data JPA + PostgreSQL
- Spring Security + stateless JWT (jjwt 0.12.6), BCrypt password hashing
- Spring `RestClient` for the predictor calls

## Configuration

All secrets and environment-specific values come from environment variables
(dev defaults live in [application.yml](src/main/resources/application.yml)).
Copy [.env.example](.env.example) to `.env` — `.env` is gitignored, never commit it.

| Variable | Purpose |
| --- | --- |
| `SERVER_PORT` | HTTP port (Render injects `PORT`; the Dockerfile maps it) |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | PostgreSQL connection |
| `PREDICTOR_URL` | Base URL of the Python prediction service |
| `PREDICTOR_API_KEY` | Sent as `X-API-Key` on every predictor call |
| `JWT_SECRET` | HS256 signing key (≥ 32 bytes) |
| `JWT_EXPIRATION_MS` | Token lifetime (default 24h) |
| `CORS_ALLOWED_ORIGINS` | Frontend origin(s), comma-separated |

A Neon JDBC URL carries no credentials — keep them in the username/password variables:

```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://<endpoint>.neon.tech/neondb?sslmode=require
```

## Build and run

```bash
./mvnw clean verify                 # compile + run the test suite
set -a; . ./.env; set +a            # load config (bash/git-bash)
java -jar target/trading-backend-0.0.1-SNAPSHOT.jar
```

PowerShell equivalent for loading `.env`:

```powershell
Get-Content .env | Where-Object { $_ -match '^\s*[^#].*=' } | ForEach-Object {
    $name, $value = $_ -split '=', 2
    [Environment]::SetEnvironmentVariable($name.Trim(), $value.Trim())
}
```

For a local database instead of a hosted one: `docker compose up -d` starts
PostgreSQL 16 on `localhost:5432` with database `trading`.

Tests run against in-memory H2 with the predictor mocked, so they need neither.

## API

Base path `/api`. Anonymous users can browse and request predictions; an account
is needed for watchlists, saved history and alerts.

| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | — | `201` + token (auto-login) |
| POST | `/api/auth/login` | — | `200` + token |
| GET | `/api/stocks` | — | curated catalog |
| GET | `/api/stocks/{ticker}/history?limit=` | — | proxied OHLCV |
| GET | `/api/predict/{ticker}` | optional | logs to the caller's history when a JWT is present |
| GET | `/api/me` | JWT | current user |
| GET/POST | `/api/watchlist` | JWT | `409` on duplicate ticker |
| DELETE | `/api/watchlist/{ticker}` | JWT | `204` |
| GET | `/api/predictions/history` | JWT | caller's rows, newest first |
| GET/POST | `/api/alerts` | JWT | |
| DELETE | `/api/alerts/{id}` | JWT | owner only |

Failures return a consistent body: `{ timestamp, status, error, message, path }`.
Predictor failures map to `422` (bad input), `503` (predictor down/cold) and
`502` (our misconfiguration), per BUILD_SPEC §3.

`AlertChecker` runs on `alerts.check-interval-ms` (default 5 min), compares each
un-triggered alert against the latest close and flips `triggered`. Delivering a
notification is out of scope.

## Deployment

Multi-stage [Dockerfile](Dockerfile) (Maven build → JRE runtime). On Render, deploy
as a Docker web service alongside a PostgreSQL instance, set every variable above,
and add the frontend origin to `CORS_ALLOWED_ORIGINS`.
