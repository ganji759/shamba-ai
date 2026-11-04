"""Fine-tune a small timm backbone. Built to run on a laptop CPU or a single GPU."""

from __future__ import annotations

import json
import math
import time
from dataclasses import asdict, dataclass
from pathlib import Path

import torch
from torch import nn
from torch.utils.data import DataLoader

from shamba_ml.config import load_config
from shamba_ml.data.dataset import ManifestDataset, eval_transform, train_transform
from shamba_ml.metrics import report
from shamba_ml.model import IMAGENET_MEAN, IMAGENET_STD, create_backbone


@dataclass
class TrainArgs:
    config: str
    data: str
    out: str
    backbone: str
    epochs: int
    batch_size: int
    lr: float
    workers: int
    limit: int = 0


@torch.no_grad()
def predict(model: nn.Module, loader: DataLoader, device) -> tuple[list[int], list[int]]:
    model.eval()
    y_true, y_pred = [], []
    for x, y in loader:
        logits = model(x.to(device, memory_format=torch.channels_last))
        y_pred += logits.argmax(1).tolist()
        y_true += y.tolist()
    return y_true, y_pred


def run(a: TrainArgs) -> Path:
    cfg = load_config(a.config)
    torch.manual_seed(cfg.seed)
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    out = Path(a.out)
    out.mkdir(parents=True, exist_ok=True)

    train_ds = ManifestDataset(
        a.data, "train", cfg.classes, train_transform(cfg.image_size), a.limit
    )
    val_ds = ManifestDataset(
        a.data,
        "val",
        cfg.classes,
        eval_transform(cfg.image_size, cfg.resize_short_side),
        a.limit // 4 if a.limit else 0,
    )
    print(f"train {len(train_ds)}  val {len(val_ds)}  classes {cfg.num_classes}  device {device}")

    # Balance classes: sources have different sizes (beans ~430 per class, others ~800).
    counts = torch.bincount(torch.tensor(train_ds.labels()), minlength=cfg.num_classes).float()
    weights = (1.0 / counts.clamp(min=1))[torch.tensor(train_ds.labels())]
    sampler = torch.utils.data.WeightedRandomSampler(weights, num_samples=len(train_ds))
    kw = dict(num_workers=a.workers, persistent_workers=a.workers > 0)
    train_dl = DataLoader(train_ds, a.batch_size, sampler=sampler, drop_last=True, **kw)
    val_dl = DataLoader(val_ds, a.batch_size * 2, **kw)

    model = create_backbone(a.backbone, cfg.num_classes).to(
        device, memory_format=torch.channels_last
    )
    opt = torch.optim.AdamW(model.parameters(), lr=a.lr, weight_decay=0.05)
    steps = a.epochs * len(train_dl)
    warmup = min(200, steps // 10)
    sched = torch.optim.lr_scheduler.LambdaLR(
        opt,
        lambda s: (
            (s + 1) / warmup
            if s < warmup
            else 0.5 * (1 + math.cos(math.pi * (s - warmup) / max(1, steps - warmup)))
        ),
    )
    loss_fn = nn.CrossEntropyLoss(label_smoothing=0.1)

    history, best_f1 = [], -1.0
    for epoch in range(a.epochs):
        model.train()
        t0, seen, loss_sum = time.time(), 0, 0.0
        for step, (x, y) in enumerate(train_dl):
            x = x.to(device, memory_format=torch.channels_last)
            y = y.to(device)
            loss = loss_fn(model(x), y)
            opt.zero_grad(set_to_none=True)
            loss.backward()
            opt.step()
            sched.step()
            seen += len(y)
            loss_sum += loss.item() * len(y)
            if step % 25 == 0:
                print(
                    f"  ep {epoch} step {step}/{len(train_dl)} loss {loss.item():.3f} "
                    f"{seen / (time.time() - t0):.1f} img/s",
                    flush=True,
                )
        y_true, y_pred = predict(model, val_dl, device)
        val = report(y_true, y_pred, cfg.classes)["summary"]
        row = {
            "epoch": epoch,
            "train_loss": loss_sum / max(1, seen),
            **{f"val_{k}": v for k, v in val.items()},
            "seconds": round(time.time() - t0, 1),
        }
        history.append(row)
        print(json.dumps(row), flush=True)
        if val["macro_f1"] > best_f1:
            best_f1 = val["macro_f1"]
            torch.save(
                {
                    "state_dict": model.state_dict(),
                    "backbone": a.backbone,
                    "classes": cfg.classes,
                    "image_size": cfg.image_size,
                    "resize_short_side": cfg.resize_short_side,
                    "mean": IMAGENET_MEAN,
                    "std": IMAGENET_STD,
                    "config_name": cfg.name,
                    "epoch": epoch,
                    "val": val,
                    "train_args": asdict(a),
                },
                out / "best.pt",
            )
    (out / "history.json").write_text(json.dumps(history, indent=2))
    print(f"best val macro F1 {best_f1:.4f} -> {out / 'best.pt'}")
    return out / "best.pt"
