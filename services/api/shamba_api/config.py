"""Settings read from environment variables."""

from __future__ import annotations

import os
from dataclasses import dataclass, field
from pathlib import Path

_PARTS = Path(__file__).resolve().parents
DEFAULT_KNOWLEDGE = _PARTS[min(3, len(_PARTS) - 1)] / "knowledge" / "diseases.json"


def _split(value: str) -> list[str]:
    return [part.strip() for part in value.split(",") if part.strip()]


@dataclass
class Settings:
    database_url: str = "sqlite:///./shamba.db"
    knowledge_path: str = str(DEFAULT_KNOWLEDGE)
    api_keys: list[str] = field(default_factory=list)
    cors_origins: list[str] = field(default_factory=list)
    model_manifest_url: str = ""
    manifest_ttl_seconds: int = 600
    k_anonymity: int = 5

    @classmethod
    def from_env(cls) -> Settings:
        env = os.environ
        return cls(
            database_url=env.get("DATABASE_URL", cls.database_url),
            knowledge_path=env.get("KNOWLEDGE_PATH", cls.knowledge_path),
            api_keys=_split(env.get("API_KEYS", "")),
            cors_origins=_split(env.get("CORS_ORIGINS", "")),
            model_manifest_url=env.get("MODEL_MANIFEST_URL", ""),
        )
