"""Model manifest source with an in-memory cache."""

from __future__ import annotations

import json
import threading
import time
from pathlib import Path

import httpx


class ManifestError(Exception):
    pass


class ManifestCache:
    def __init__(self, source: str, ttl: int = 600) -> None:
        self.source = source
        self.ttl = ttl
        self._value: dict | None = None
        self._fetched = 0.0
        self._lock = threading.Lock()

    def _fetch(self) -> dict:
        if self.source.startswith(("http://", "https://")):
            response = httpx.get(self.source, timeout=10, follow_redirects=True)
            response.raise_for_status()
            return response.json()
        return json.loads(Path(self.source).read_text(encoding="utf-8"))

    def get(self) -> dict:
        if not self.source:
            raise ManifestError("MODEL_MANIFEST_URL is not set")
        with self._lock:
            now = time.monotonic()
            if self._value is not None and now - self._fetched < self.ttl:
                return self._value
            try:
                self._value = self._fetch()
                self._fetched = now
            except (httpx.HTTPError, OSError, ValueError) as exc:
                if self._value is None:
                    raise ManifestError(f"cannot load manifest: {exc}") from exc
                # Serve the stale copy and retry on the next request after a short wait.
                self._fetched = now - self.ttl + 30
            return self._value
