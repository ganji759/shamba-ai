"""Evaluate a checkpoint on a manifest split."""

from __future__ import annotations

import json
from pathlib import Path

import torch
from torch.utils.data import DataLoader

from shamba_ml.data.dataset import ManifestDataset, eval_transform
from shamba_ml.metrics import report
from shamba_ml.model import load_checkpoint
from shamba_ml.train import predict


def run(checkpoint: str, data: str, split: str, out: str | None) -> dict:
    model, ckpt = load_checkpoint(checkpoint)
    model = model.to(memory_format=torch.channels_last)
    ds = ManifestDataset(
        data, split, ckpt["classes"], eval_transform(ckpt["image_size"], ckpt["resize_short_side"])
    )
    y_true, y_pred = predict(model, DataLoader(ds, 64, num_workers=4), torch.device("cpu"))
    rep = report(y_true, y_pred, ckpt["classes"])
    rep["split"] = split
    rep["checkpoint_epoch"] = ckpt["epoch"]
    if out:
        Path(out).parent.mkdir(parents=True, exist_ok=True)
        Path(out).write_text(json.dumps(rep, indent=2))
    return rep
