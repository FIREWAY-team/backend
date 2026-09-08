.PHONY: dev test lint format db-up migrate
dev:
	uv run uvicorn src.main:app --reload
test:
	uv run pytest
lint:
	uv run ruff check .
format:
	uv run ruff format .
db-up:
	docker compose up -d postgis
migrate:
	uv run alembic upgrade head

