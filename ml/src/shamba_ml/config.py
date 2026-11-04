"""Typed access to the YAML model config."""

from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path

import yaml


@dataclass
class Config:
    name: str
    classes: list[str]
    image_size: int = 224
    resize_short_side: int = 256
    seed: int = 42
    split: dict = field(default_factory=lambda: {"val": 0.1, "test": 0.1})
    sources: list[dict] = field(default_factory=list)

    @property
    def num_classes(self) -> int:
        return len(self.classes)

    def crop_of(self, class_id: str) -> str:
        return class_id.split("_", 1)[0]


def load_config(path: str | Path) -> Config:
    data = yaml.safe_load(Path(path).read_text())
    cfg = Config(**data)
    if len(set(cfg.classes)) != len(cfg.classes):
        raise ValueError("class ids must be unique")
    for src in cfg.sources:
        unknown = set(src["label_map"].values()) - set(cfg.classes)
        if unknown:
            raise ValueError(f"source {src['id']} maps to unknown classes {unknown}")
    return cfg
