"""Outbreak aggregation with k-anonymity."""

from __future__ import annotations

from datetime import UTC, datetime, timedelta

from sqlalchemy import distinct, func, select
from sqlalchemy.orm import Session

from . import geohash as gh
from .db import Observation
from .knowledge import Knowledge
from .schemas import OutbreakReport, OutbreakRow

TREND_THRESHOLD_PCT = 20.0


def _trend(count: int, prev: int) -> str:
    if prev == 0:
        return "new"
    pct = (count - prev) / prev * 100
    if pct >= TREND_THRESHOLD_PCT:
        return "rising"
    if pct <= -TREND_THRESHOLD_PCT:
        return "falling"
    return "stable"


def _grouped(session: Session, start: datetime, end: datetime, crop: str | None):
    stmt = (
        select(
            Observation.geohash,
            Observation.class_id,
            Observation.crop,
            func.count(Observation.id).label("n"),
            func.count(distinct(Observation.install_id)).label("installs"),
        )
        .where(
            Observation.geohash.is_not(None),
            Observation.captured_at >= start,
            Observation.captured_at < end,
            Observation.class_id.not_like("%\\_healthy", escape="\\"),
        )
        .group_by(Observation.geohash, Observation.class_id, Observation.crop)
    )
    if crop:
        stmt = stmt.where(Observation.crop == crop)
    return stmt


def build_report(
    session: Session, knowledge: Knowledge, k: int, crop: str | None, days: int
) -> OutbreakReport:
    now = datetime.now(UTC).replace(tzinfo=None)
    start = now - timedelta(days=days)
    prev_start = start - timedelta(days=days)

    current = session.execute(_grouped(session, start, now + timedelta(days=1), crop)).all()
    previous = {
        (r.geohash, r.class_id): r.n
        for r in session.execute(_grouped(session, prev_start, start, crop)).all()
    }

    rows: list[OutbreakRow] = []
    for r in current:
        if r.installs < k:
            continue  # k-anonymity: hide cells with fewer than k distinct installs
        prev = previous.get((r.geohash, r.class_id), 0)
        lat, lon = gh.decode(r.geohash)
        rows.append(
            OutbreakRow(
                geohash=r.geohash,
                lat=round(lat, 3),
                lon=round(lon, 3),
                class_id=r.class_id,
                crop=r.crop,
                count=r.n,
                installs=r.installs,
                previous_count=prev,
                change=r.n - prev,
                change_pct=round((r.n - prev) / prev * 100, 1) if prev else None,
                trend=_trend(r.n, prev),
            )
        )
    rows.sort(key=lambda x: (-x.count, x.geohash, x.class_id))
    return OutbreakReport(
        days=days,
        crop=crop,
        k_anonymity=k,
        period_start=start,
        period_end=now,
        rows=rows,
    )


CSV_FIELDS = [
    "geohash",
    "lat",
    "lon",
    "class_id",
    "crop",
    "count",
    "installs",
    "previous_count",
    "change",
    "change_pct",
    "trend",
]
