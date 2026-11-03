import json
from uuid import uuid4

from sqlalchemy import select
from sqlalchemy.orm import Session

from shamba_api.db import Observation
from tests.conftest import KEY, obs, post

H = {"X-API-Key": KEY}


def seed(client, n, **kw):
    r = post(client, [obs(**kw) for _ in range(n)])
    assert r.status_code == 200, r.text


def test_health(client):
    assert client.get("/health").json() == {"status": "ok"}


def test_validation(client):
    assert post(client, [obs(confidence=1.5)]).status_code == 422
    assert post(client, [obs(status="maybe")]).status_code == 422
    assert post(client, [obs(platform="ios")]).status_code == 422
    assert post(client, [obs(install_id="not-a-uuid")]).status_code == 422
    assert post(client, [obs(class_id="made_up_disease")]).status_code == 422
    assert post(client, [obs(email="a@b.c")]).status_code == 422  # extra fields refused
    assert post(client, [obs(geohash="aaaa!")]).status_code == 422
    assert post(client, []).status_code == 422
    assert post(client, [obs() for _ in range(101)]).status_code == 422
    assert post(client, [obs() for _ in range(100)]).status_code == 200


def test_idempotency(client, tmp_path):
    item = obs()
    first = post(client, [item, item]).json()
    assert first == {"received": 2, "accepted": 1, "duplicates": 1}
    again = post(client, [item]).json()
    assert again == {"received": 1, "accepted": 0, "duplicates": 1}
    other_class = dict(item, class_id="maize_streak_virus")
    assert post(client, [other_class]).json()["accepted"] == 1


def test_geohash_truncated(client, tmp_path):
    assert post(client, [obs(geohash="kzf0w9xbcde")]).status_code == 200
    assert post(client, [obs(geohash=None)]).status_code == 200
    from shamba_api.db import make_engine

    eng = make_engine(client.app.state.settings.database_url)
    with Session(eng) as s:
        values = {o.geohash for o in s.scalars(select(Observation))}
    assert values == {"kzf0w", None}


def test_auth(client):
    assert client.get("/v1/outbreaks").status_code == 401
    assert client.get("/v1/outbreaks", headers={"X-API-Key": "bad"}).status_code == 401
    assert client.get("/v1/outbreaks", headers=H).status_code == 200
    assert client.get("/v1/outbreaks.csv").status_code == 401
    assert client.get("/dashboard").status_code == 401
    assert client.get("/dashboard", params={"key": KEY}).status_code == 200
    assert client.get("/dashboard", headers={"X-API-Key": "other"}).status_code == 200
    assert client.post("/v1/observations", json={"observations": [obs()]}).status_code == 200


def test_k_anonymity(client):
    seed(client, 3)
    assert client.get("/v1/outbreaks", headers=H).json()["rows"] == []
    # Same install repeated does not count as a new install.
    same = uuid4()
    post(client, [obs(install_id=same, sec=s) for s in range(1, 10)])
    assert client.get("/v1/outbreaks", headers=H).json()["rows"] == []
    seed(client, 1)
    rows = client.get("/v1/outbreaks", headers=H).json()["rows"]
    assert len(rows) == 1
    assert rows[0]["geohash"] == "kzf0w"
    assert rows[0]["installs"] == 5
    assert rows[0]["count"] == 13
    assert rows[0]["trend"] == "new"


def test_trend_and_filters(client):
    seed(client, 5, days_ago=2)
    seed(client, 5, days_ago=40)
    seed(client, 5, days_ago=3, class_id="bean_rust", crop="bean")
    seed(client, 6, class_id="maize_healthy", days_ago=2)
    rows = client.get("/v1/outbreaks", headers=H).json()["rows"]
    mln = next(r for r in rows if r["class_id"] == "maize_lethal_necrosis")
    assert (mln["count"], mln["previous_count"], mln["trend"]) == (5, 5, "stable")
    assert all("healthy" not in r["class_id"] for r in rows)
    only = client.get("/v1/outbreaks", headers=H, params={"crop": "bean"}).json()["rows"]
    assert [r["class_id"] for r in only] == ["bean_rust"]
    assert client.get("/v1/outbreaks", headers=H, params={"crop": "kale"}).status_code == 422
    assert client.get("/v1/outbreaks", headers=H, params={"days": 0}).status_code == 422


def test_csv_and_dashboard(client):
    seed(client, 6)
    csv_text = client.get("/v1/outbreaks.csv", headers=H).text
    lines = csv_text.strip().splitlines()
    assert lines[0].startswith("geohash,lat,lon,class_id")
    assert len(lines) == 2
    html = client.get("/dashboard", params={"key": KEY}).text
    assert "Maize lethal necrosis" in html
    assert "critical alerts" in html
    assert "http" not in html.replace("http-equiv", "")


def test_manifest(client, tmp_path):
    assert client.get("/v1/models/latest").status_code == 503
    path = tmp_path / "m.json"
    path.write_text(json.dumps({"version": "1.2.3"}))
    from shamba_api.manifest import ManifestCache

    cache = ManifestCache(str(path), ttl=600)
    assert cache.get()["version"] == "1.2.3"
    path.write_text(json.dumps({"version": "9"}))
    assert cache.get()["version"] == "1.2.3"  # cached
    cache.ttl = 0
    assert cache.get()["version"] == "9"
