"""FastAPI application."""

import csv
import hmac
import io
from collections.abc import Iterator
from contextlib import asynccontextmanager
from datetime import UTC, datetime
from typing import Annotated

from fastapi import Depends, FastAPI, Header, HTTPException, Query, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse, Response
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session, sessionmaker

from . import dashboard as dash
from .config import Settings
from .db import Base, Observation, make_engine
from .knowledge import Knowledge
from .manifest import ManifestCache, ManifestError
from .outbreaks import CSV_FIELDS, build_report
from .schemas import BatchResult, ObservationBatch, OutbreakReport


def create_app(settings: Settings | None = None) -> FastAPI:
    settings = settings or Settings.from_env()
    knowledge = Knowledge.load(settings.knowledge_path)
    engine = make_engine(settings.database_url)
    session_factory = sessionmaker(engine, expire_on_commit=False)
    manifest = ManifestCache(settings.model_manifest_url, settings.manifest_ttl_seconds)

    @asynccontextmanager
    async def lifespan(_: FastAPI):
        Base.metadata.create_all(engine)  # use Alembic migrations later
        yield
        engine.dispose()

    app = FastAPI(title="Shamba AI API", version="0.1.0", lifespan=lifespan)
    app.state.settings = settings
    if settings.cors_origins:
        app.add_middleware(
            CORSMiddleware,
            allow_origins=settings.cors_origins,
            allow_methods=["GET", "POST"],
            allow_headers=["Content-Type", "X-API-Key"],
        )

    def get_session() -> Iterator[Session]:
        with session_factory() as session:
            yield session

    SessionDep = Annotated[Session, Depends(get_session)]

    def require_key(
        x_api_key: Annotated[str | None, Header()] = None,
        key: Annotated[str | None, Query()] = None,
    ) -> str:
        supplied = x_api_key or key or ""
        ok = False
        for valid in settings.api_keys:  # check every key, no early exit
            ok |= hmac.compare_digest(supplied.encode(), valid.encode())
        if not supplied or not ok:
            raise HTTPException(401, "invalid or missing API key")
        return supplied

    AuthDep = Annotated[str, Depends(require_key)]

    @app.get("/health")
    def health() -> dict:
        return {"status": "ok"}

    @app.get("/v1/models/latest")
    def latest_model() -> dict:
        try:
            return manifest.get()
        except ManifestError as exc:
            raise HTTPException(503, str(exc)) from exc

    @app.post("/v1/observations", response_model=BatchResult)
    def post_observations(batch: ObservationBatch, session: SessionDep) -> BatchResult:
        bad_classes = sorted({o.class_id for o in batch.observations} - knowledge.classes)
        bad_crops = sorted({o.crop for o in batch.observations} - knowledge.crops)
        if bad_classes or bad_crops:
            raise HTTPException(422, {"unknown_class_ids": bad_classes, "unknown_crops": bad_crops})

        now = datetime.now(UTC).replace(tzinfo=None)
        rows: dict[tuple, Observation] = {}
        for o in batch.observations:
            captured = o.captured_at.astimezone(UTC).replace(tzinfo=None)
            key = (str(o.install_id), captured, o.class_id)
            if key in rows:
                continue
            rows[key] = Observation(
                install_id=key[0],
                class_id=o.class_id,
                crop=o.crop,
                confidence=o.confidence,
                status=o.status,
                model_version=o.model_version,
                app_version=o.app_version,
                platform=o.platform,
                captured_at=captured,
                geohash=o.geohash,
                received_at=now,
            )

        installs = {k[0] for k in rows}
        existing = set(
            session.execute(
                select(Observation.install_id, Observation.captured_at, Observation.class_id).where(
                    Observation.install_id.in_(installs)
                )
            ).all()
        )
        fresh = [row for key, row in rows.items() if key not in existing]
        accepted = 0
        try:
            session.add_all(fresh)
            session.commit()
            accepted = len(fresh)
        except IntegrityError:
            # A concurrent request inserted the same rows. Fall back to one by one.
            session.rollback()
            for row in fresh:
                session.add(
                    Observation(
                        **{
                            c.name: getattr(row, c.name)
                            for c in Observation.__table__.c
                            if c.name != "id"
                        }
                    )
                )
                try:
                    session.commit()
                    accepted += 1
                except IntegrityError:
                    session.rollback()
        total = len(batch.observations)
        return BatchResult(received=total, accepted=accepted, duplicates=total - accepted)

    def report_for(session: Session, crop: str | None, days: int) -> OutbreakReport:
        if crop and crop not in knowledge.crops:
            raise HTTPException(422, f"unknown crop: {crop}")
        return build_report(session, knowledge, settings.k_anonymity, crop, days)

    Days = Annotated[int, Query(ge=1, le=365)]

    @app.get("/v1/outbreaks", response_model=OutbreakReport)
    def outbreaks(
        _: AuthDep, session: SessionDep, crop: str | None = None, days: Days = 30
    ) -> OutbreakReport:
        return report_for(session, crop, days)

    @app.get("/v1/outbreaks.csv")
    def outbreaks_csv(
        _: AuthDep, session: SessionDep, crop: str | None = None, days: Days = 30
    ) -> Response:
        report = report_for(session, crop, days)
        buf = io.StringIO()
        writer = csv.DictWriter(buf, fieldnames=CSV_FIELDS)
        writer.writeheader()
        for row in report.rows:
            writer.writerow(row.model_dump(include=set(CSV_FIELDS)))
        return Response(
            buf.getvalue(),
            media_type="text/csv",
            headers={"Content-Disposition": 'attachment; filename="outbreaks.csv"'},
        )

    @app.get("/dashboard", response_class=HTMLResponse)
    def dashboard(
        request: Request,
        key: AuthDep,
        session: SessionDep,
        crop: str | None = None,
        days: Days = 30,
    ) -> HTMLResponse:
        report = report_for(session, crop, days)
        from_query = "key" in request.query_params
        html = dash.render(report, knowledge, key if from_query else None)
        return HTMLResponse(html, headers={"Cache-Control": "no-store"})

    return app


def app_factory() -> FastAPI:
    return create_app()
