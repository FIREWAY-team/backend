"""Application settings loaded from environment and .env."""
from functools import lru_cache
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    database_url: str = "postgresql+asyncpg://goldenlane:goldenlane@localhost:5432/goldenlane"
    log_level: str = "INFO"
    env: str = "development"
    external_fire_system_url: str = "http://localhost:8081"
    health_check_db: bool = False
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

@lru_cache
def get_settings() -> Settings:
    return Settings()

