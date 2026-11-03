"""`shamba` command line: prepare data, train, evaluate, export, predict, benchmark."""

from __future__ import annotations

import json
from pathlib import Path

import typer

app = typer.Typer(add_completion=False, help="Shamba AI on-device crop-health model tools.")

DEFAULT_CONFIG = "configs/crops-v1.yaml"


@app.command()
def prepare(
    config: str = DEFAULT_CONFIG,
    out: str = "data/processed",
    only: str = typer.Option("", help="Comma list of source ids. Empty = all."),
    merge_only: bool = typer.Option(False, help="Only merge the per-source manifests."),
):
    """Download field photos from the Hugging Face Hub and build the local image set."""
    from shamba_ml.data import prepare as prep

    ids = [s for s in only.split(",") if s]
    if merge_only:
        ids = ["__none__"]
    prep.run(config, out, ids or None)


@app.command()
def train(
    config: str = DEFAULT_CONFIG,
    data: str = "data/processed",
    out: str = "runs/v1",
    backbone: str = "mobilenetv4_conv_small.e2400_r224_in1k",
    epochs: int = 8,
    batch_size: int = 32,
    lr: float = 2e-3,
    workers: int = 4,
    limit: int = typer.Option(0, help="Use only N train images (smoke test)."),
):
    """Fine-tune a timm backbone on the prepared image set."""
    from shamba_ml.train import TrainArgs, run

    run(TrainArgs(config, data, out, backbone, epochs, batch_size, lr, workers, limit))


@app.command()
def evaluate(
    checkpoint: str = "runs/v1/best.pt",
    data: str = "data/processed",
    split: str = "test",
    out: str = "runs/v1/metrics.json",
):
    """Accuracy, macro F1 and confusion matrix on a split."""
    from shamba_ml.evaluate import run

    print(json.dumps(run(checkpoint, data, split, out)["summary"], indent=2))


@app.command()
def export(
    checkpoint: str = "runs/v1/best.pt",
    out: str = "artifacts",
    targets: str = typer.Option("xnnpack,xnnpack-int8,webgpu,portable", help="Comma list."),
    calib_data: str = "data/processed",
):
    """Export the model to ExecuTorch .pte programs and write manifest.json."""
    from shamba_ml.export import run

    run(checkpoint, out, [t for t in targets.split(",") if t], calib_data)


@app.command()
def predict(
    image: Path,
    model: str = "artifacts/shamba_crop_health_xnnpack_int8.pte",
    manifest: str = "artifacts/manifest.json",
    knowledge: str = "../knowledge/diseases.json",
    lang: str = "en",
    top_k: int = 3,
):
    """Diagnose one photo offline with the ExecuTorch runtime."""
    from shamba_ml.infer import Diagnoser

    d = Diagnoser(model, manifest, knowledge)
    print(json.dumps(d.diagnose(image, lang=lang, top_k=top_k), indent=2, ensure_ascii=False))


@app.command()
def bench(
    models: str = "artifacts",
    runs: int = 30,
):
    """Measure .pte latency on this machine with the ExecuTorch runtime."""
    from shamba_ml.bench import run

    print(json.dumps(run(models, runs), indent=2))


if __name__ == "__main__":
    app()
