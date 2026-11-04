"""Allow-list of classes and crops, loaded from knowledge/diseases.json."""

from __future__ import annotations

import json
from dataclasses import dataclass, field
from pathlib import Path

DEFAULT_CRITICAL = frozenset(
    {"maize_lethal_necrosis", "cassava_brown_streak", "banana_fusarium_wilt"}
)


@dataclass
class Knowledge:
    classes: frozenset[str] = frozenset()
    crops: frozenset[str] = frozenset()
    critical: frozenset[str] = DEFAULT_CRITICAL
    names: dict[str, str] = field(default_factory=dict)

    @classmethod
    def load(cls, path: str | Path) -> Knowledge:
        data = json.loads(Path(path).read_text(encoding="utf-8"))
        conditions = data["conditions"]
        crops = data.get("crops", [])
        crop_ids = set(crops if isinstance(crops, list) else crops.keys())
        crop_ids.update(c.get("crop") for c in conditions.values() if c.get("crop"))
        critical = set(DEFAULT_CRITICAL)
        critical.update(k for k, v in conditions.items() if v.get("urgency") == "critical")
        names = {}
        for key, value in conditions.items():
            name = value.get("name")
            names[key] = name.get("en", key) if isinstance(name, dict) else key
        return cls(
            classes=frozenset(conditions),
            crops=frozenset(crop_ids),
            critical=frozenset(critical),
            names=names,
        )

    def is_healthy(self, class_id: str) -> bool:
        return class_id.endswith("_healthy")
