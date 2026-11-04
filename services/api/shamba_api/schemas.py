"""Pydantic models for requests and responses."""

from __future__ import annotations

from datetime import UTC, datetime, timedelta
from typing import Literal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field, field_validator

from . import geohash as gh

MAX_BATCH = 100


class ObservationIn(BaseModel):
    model_config = ConfigDict(extra="forbid")

    install_id: UUID
    class_id: str = Field(min_length=1, max_length=64, pattern=r"^[a-z0-9_]+$")
    crop: str = Field(min_length=1, max_length=32, pattern=r"^[a-z0-9_]+$")
    confidence: float = Field(ge=0, le=1)
    status: Literal["confident", "likely", "uncertain"]
    model_version: str = Field(min_length=1, max_length=64)
    app_version: str = Field(min_length=1, max_length=64)
    platform: Literal["web", "android", "desktop"]
    captured_at: datetime
    geohash: str | None = Field(default=None, max_length=12)

    @field_validator("captured_at")
    @classmethod
    def _utc(cls, value: datetime) -> datetime:
        if value.tzinfo is None:
            value = value.replace(tzinfo=UTC)
        value = value.astimezone(UTC)
        if value > datetime.now(UTC) + timedelta(days=1):
            raise ValueError("captured_at is in the future")
        return value

    @field_validator("geohash")
    @classmethod
    def _geohash(cls, value: str | None) -> str | None:
        if value is None or value == "":
            return None
        value = value.lower()
        if not gh.is_valid(value) or len(value) < gh.PRECISION:
            raise ValueError("geohash must be 5 to 12 valid geohash characters")
        return gh.truncate(value)


class ObservationBatch(BaseModel):
    model_config = ConfigDict(extra="forbid")

    observations: list[ObservationIn] = Field(min_length=1, max_length=MAX_BATCH)


class BatchResult(BaseModel):
    received: int
    accepted: int
    duplicates: int


class OutbreakRow(BaseModel):
    geohash: str
    lat: float
    lon: float
    class_id: str
    crop: str
    count: int
    installs: int
    previous_count: int
    change: int
    change_pct: float | None
    trend: Literal["rising", "falling", "stable", "new"]


class OutbreakReport(BaseModel):
    days: int
    crop: str | None
    k_anonymity: int
    period_start: datetime
    period_end: datetime
    rows: list[OutbreakRow]
