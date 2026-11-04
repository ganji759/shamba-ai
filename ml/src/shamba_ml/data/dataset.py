"""Torch dataset over manifest.csv. Train-time augmentation and eval preprocessing."""

from __future__ import annotations

import csv
from pathlib import Path

import torch
from PIL import Image
from torch.utils.data import Dataset
from torchvision.transforms import v2 as T

from shamba_ml.model import IMAGENET_MEAN, IMAGENET_STD


def train_transform(size: int) -> T.Compose:
    return T.Compose(
        [
            T.RandomResizedCrop(size, scale=(0.35, 1.0), antialias=True),
            T.RandomHorizontalFlip(),
            T.RandomVerticalFlip(),
            T.RandomApply([T.RandomRotation(25)], p=0.3),
            # Field photos vary a lot in light: sun, shade, phone white balance.
            T.ColorJitter(brightness=0.35, contrast=0.3, saturation=0.3, hue=0.03),
            T.RandomApply([T.GaussianBlur(5, sigma=(0.1, 1.5))], p=0.2),
            T.ToImage(),
            T.ToDtype(torch.float32, scale=True),
            T.Normalize(IMAGENET_MEAN, IMAGENET_STD),
            T.RandomErasing(p=0.15),
        ]
    )


def eval_transform(size: int, resize: int, normalize: bool = True) -> T.Compose:
    """Same steps every client app runs: short side -> `resize`, center crop `size`."""
    steps = [
        T.Resize(resize, antialias=True),
        T.CenterCrop(size),
        T.ToImage(),
        T.ToDtype(torch.float32, scale=True),
    ]
    if normalize:
        steps.append(T.Normalize(IMAGENET_MEAN, IMAGENET_STD))
    return T.Compose(steps)


class ManifestDataset(Dataset):
    def __init__(self, root: str | Path, split: str, classes: list[str], transform, limit: int = 0):
        self.root = Path(root)
        self.index = {c: i for i, c in enumerate(classes)}
        with (self.root / "manifest.csv").open() as fh:
            rows = [
                r for r in csv.DictReader(fh) if r["split"] == split and r["label"] in self.index
            ]
        if limit:
            rows = rows[:: max(1, len(rows) // limit)][:limit]
        self.rows = rows
        self.transform = transform

    def __len__(self) -> int:
        return len(self.rows)

    def labels(self) -> list[int]:
        return [self.index[r["label"]] for r in self.rows]

    def __getitem__(self, i: int):
        r = self.rows[i]
        img = Image.open(self.root / r["path"]).convert("RGB")
        # contiguous(): ExecuTorch reads raw NCHW memory and ignores strides.
        return self.transform(img).contiguous(), self.index[r["label"]]
