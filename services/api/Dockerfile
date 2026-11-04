# syntax=docker/dockerfile:1
# Build from the repository root so the knowledge file is included:
#   docker build -f services/api/Dockerfile -t shamba-api .
FROM ghcr.io/astral-sh/uv:python3.12-bookworm-slim AS build
ENV UV_COMPILE_BYTECODE=1 UV_LINK_MODE=copy UV_PYTHON_DOWNLOADS=never
WORKDIR /app
COPY services/api/pyproject.toml services/api/uv.lock services/api/.python-version ./
RUN --mount=type=cache,target=/root/.cache/uv uv sync --frozen --no-dev --no-install-project
COPY services/api/shamba_api ./shamba_api

FROM python:3.12-slim-bookworm
ENV PYTHONUNBUFFERED=1 PATH="/app/.venv/bin:$PATH" \
    KNOWLEDGE_PATH=/app/knowledge/diseases.json DATABASE_URL=sqlite:////tmp/shamba.db
RUN useradd --uid 10001 --no-create-home shamba
WORKDIR /app
COPY --from=build /app /app
COPY knowledge/diseases.json /app/knowledge/diseases.json
USER 10001
EXPOSE 8080
CMD ["uvicorn", "shamba_api.main:app_factory", "--factory", "--host", "0.0.0.0", "--port", "8080"]
