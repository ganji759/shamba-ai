"""Database models and engine setup.

Note: tables are created with create_all on startup. For schema changes in
production, add Alembic migrations.
"""

from __future__ import annotations

from datetime import datetime

from sqlalchemy import DateTime, Float, Index, String, UniqueConstraint, create_engine
from sqlalchemy.engine import Engine
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column
from sqlalchemy.pool import StaticPool


class Base(DeclarativeBase):
    pass


class Observation(Base):
    __tablename__ = "observations"
    __table_args__ = (
        UniqueConstraint("install_id", "captured_at", "class_id", name="uq_observation"),
        Index("ix_obs_geo_class_time", "geohash", "class_id", "captured_at"),
        Index("ix_obs_crop_time", "crop", "captured_at"),
    )

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    install_id: Mapped[str] = mapped_column(String(36))
    class_id: Mapped[str] = mapped_column(String(64))
    crop: Mapped[str] = mapped_column(String(32))
    confidence: Mapped[float] = mapped_column(Float)
    status: Mapped[str] = mapped_column(String(16))
    model_version: Mapped[str] = mapped_column(String(64))
    app_version: Mapped[str] = mapped_column(String(64))
    platform: Mapped[str] = mapped_column(String(16))
    captured_at: Mapped[datetime] = mapped_column(DateTime)  # naive UTC
    geohash: Mapped[str | None] = mapped_column(String(5), nullable=True)
    received_at: Mapped[datetime] = mapped_column(DateTime)  # naive UTC


def normalize_url(url: str) -> str:
    if url.startswith("postgres://"):
        url = "postgresql://" + url[len("postgres://") :]
    if url.startswith("postgresql://"):
        url = "postgresql+psycopg://" + url[len("postgresql://") :]
    return url


def make_engine(url: str) -> Engine:
    url = normalize_url(url)
    if url.startswith("sqlite"):
        kwargs: dict = {"connect_args": {"check_same_thread": False}}
        if ":memory:" in url or url in ("sqlite://", "sqlite:///"):
            kwargs["poolclass"] = StaticPool
        return create_engine(url, **kwargs)
    return create_engine(url, pool_pre_ping=True)
