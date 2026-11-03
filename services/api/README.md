# Shamba AI API

The cloud layer is optional. Diagnosis runs on the phone. The phone sends anonymised results only when it is online. This service stores them and turns them into outbreak intelligence.

## Run

```
cd services/api
uv sync
API_KEYS=dev-key uv run python -m shamba_api
```

With Postgres: `API_KEYS=dev-key docker compose up --build` (or podman compose). The server listens on port 8080.

Test: `uv run ruff check . && uv run pytest`.

## Endpoints

| Method and path | Auth | Purpose |
|---|---|---|
| GET /health | none | Liveness check. |
| GET /v1/models/latest | none | Model manifest. Cached in memory for 10 minutes. |
| POST /v1/observations | none | Batch of 1 to 100 observations. |
| GET /v1/outbreaks?crop=&days=30 | key | Counts per area and class, with trend. |
| GET /v1/outbreaks.csv | key | Same data as CSV. |
| GET /dashboard | key | HTML page for an organisation. |

Send the key in the `X-API-Key` header. The dashboard also accepts `?key=`. A key in a URL can appear in logs and browser history. Use the header for scripts.

### POST /v1/observations

Each item has: `install_id` (UUID), `class_id`, `crop`, `confidence` (0 to 1), `status` (confident, likely, uncertain), `model_version`, `app_version`, `platform` (web, android, desktop), `captured_at` (ISO datetime) and optional `geohash` (5 to 12 characters). Unknown fields are refused. `class_id` and `crop` must exist in the knowledge file. The reply is `{received, accepted, duplicates}`. A repeat of `(install_id, captured_at, class_id)` is counted as a duplicate and not stored again.

### Outbreaks

The service counts reports per geohash cell and class for the last `days` days. It compares them with the `days` before that. The trend is `new`, `rising` (up 20% or more), `falling` (down 20% or more) or `stable`. Healthy classes are left out. A cell appears only when at least 5 distinct installs reported that class in it.

## Environment variables

| Name | Default | Meaning |
|---|---|---|
| DATABASE_URL | sqlite:///./shamba.db | SQLAlchemy URL. Use Postgres in production. `postgresql://` URLs work. |
| API_KEYS | empty | Comma separated keys. With none set, protected routes always return 401. |
| CORS_ORIGINS | empty | Comma separated origins, for example the web app URL. |
| KNOWLEDGE_PATH | knowledge/diseases.json in the repository | File whose `conditions` keys form the class allow-list. |
| MODEL_MANIFEST_URL | empty | HTTP(S) URL (for example a Hugging Face resolve URL) or a local file path. |

## Privacy design

- The install ID is a random UUID made on the device. It links to no person, phone or account.
- The API takes no names, phone numbers, IP addresses or images. Unknown fields are refused.
- Location is optional. The server cuts any geohash to 5 characters (about 5 km) before it stores it. The longer value is never stored.
- Outbreak output never shows a cell with fewer than 5 distinct installs (k-anonymity).
- Output has counts only. It has no install IDs and no single observation.
- Put the API behind your host's access log rules. Do not log request bodies.

## Database

Tables are created on startup with `create_all`. This does not change existing tables. Add Alembic migrations before the first schema change in production.
