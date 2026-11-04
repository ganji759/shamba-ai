from datetime import UTC, datetime, timedelta
from uuid import uuid4

import pytest
from fastapi.testclient import TestClient

from shamba_api.config import Settings
from shamba_api.main import create_app

KEY = "test-key"


@pytest.fixture
def client(tmp_path):
    settings = Settings(database_url=f"sqlite:///{tmp_path}/t.db", api_keys=[KEY, "other"])
    with TestClient(create_app(settings)) as c:
        yield c


def obs(
    install_id=None,
    class_id="maize_lethal_necrosis",
    crop="maize",
    days_ago=1,
    geohash="kzf0w9x",
    **kw,
):
    when = datetime.now(UTC) - timedelta(days=days_ago, seconds=kw.pop("sec", 0))
    data = {
        "install_id": str(install_id or uuid4()),
        "class_id": class_id,
        "crop": crop,
        "confidence": 0.9,
        "status": "confident",
        "model_version": "1.0.0",
        "app_version": "0.1.0",
        "platform": "android",
        "captured_at": when.isoformat(),
        "geohash": geohash,
    }
    data.update(kw)
    return data


def post(client, items):
    return client.post("/v1/observations", json={"observations": items})
