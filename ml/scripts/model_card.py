"""Render MODEL_CARD.md (the Hugging Face README) from measured results only.

    python scripts/model_card.py --metrics runs/v1/metrics.json \
        --artifacts artifacts --bench artifacts/bench.json --out MODEL_CARD.md

Every number in the card comes from a JSON file written by evaluate, export or bench.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path

from shamba_ml.config import load_config

FRONT = """---
license: apache-2.0
library_name: executorch
pipeline_tag: image-classification
tags:
- executorch
- on-device
- xnnpack
- webgpu
- agriculture
- plant-disease
- east-africa
datasets:
- AI-Lab-Makerere/beans
- Project-AgML/maize_disease_classification
- Project-AgML/banana_disease_classification_tanzania
- Engineer101/cassava-leaf-disease-classification
base_model: timm/{backbone}
---
"""


def pct(x: float) -> str:
    return f"{100 * x:.1f}%"


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--config", default="configs/crops-v1.yaml")
    ap.add_argument("--metrics", default="runs/v1/metrics.json")
    ap.add_argument("--history", default="runs/v1/history.json")
    ap.add_argument("--artifacts", default="artifacts")
    ap.add_argument("--bench", default="artifacts/bench.json")
    ap.add_argument("--data", default="data/processed")
    ap.add_argument("--out", default="MODEL_CARD.md")
    a = ap.parse_args()

    cfg = load_config(a.config)
    m = json.loads(Path(a.metrics).read_text())
    man = json.loads((Path(a.artifacts) / "manifest.json").read_text())
    bench = json.loads(Path(a.bench).read_text()) if Path(a.bench).exists() else None
    hist = json.loads(Path(a.history).read_text()) if Path(a.history).exists() else []
    s = m["summary"]
    split_text = "field photos from the sources below"
    data_manifest = Path(a.data) / "manifest.csv"
    if data_manifest.exists():
        import csv
        from collections import Counter

        with data_manifest.open() as fh:
            n = Counter(r["split"] for r in csv.DictReader(fh))
        split_text = (
            f"{sum(n.values()):,} field photos ({n['train']:,} train, "
            f"{n['val']:,} validation, {n['test']:,} test)"
        )

    out = [FRONT.format(backbone=man["backbone"])]
    out.append("# Shamba crop-health v1 (ExecuTorch)\n")
    out.append(
        "A small image classifier for leaf diseases of beans, cassava, maize and banana, "
        "trained on field photos from Uganda and Tanzania. It runs fully on device with "
        "[PyTorch ExecuTorch](https://github.com/pytorch/executorch): Android and laptops "
        "(XNNPACK) and web browsers (WebGPU, or the portable CPU kernels in WebAssembly).\n"
    )
    out.append(
        "It powers [Shamba AI](https://github.com/ganji759/shamba-ai), an offline crop-health "
        "assistant for smallholder farmers. Advice for each class is in the app's "
        "knowledge base, not in this model.\n"
    )

    out.append("## Results\n")
    out.append(
        f"Held-out test split, {s['n']} photos, {len(cfg.classes)} classes, "
        f"eager PyTorch model (epoch {m['checkpoint_epoch']}):\n"
    )
    out.append("| Metric | Value |\n|---|---|")
    out.append(f"| Accuracy | {pct(s['accuracy'])} |")
    out.append(f"| Macro F1 | {pct(s['macro_f1'])} |\n")
    out.append("Per class:\n")
    out.append("| Class | Precision | Recall | F1 | Photos |\n|---|---|---|---|---|")
    for c, r in m["per_class"].items():
        out.append(
            f"| `{c}` | {pct(r['precision'])} | {pct(r['recall'])} | {pct(r['f1'])} "
            f"| {r['support']} |"
        )
    out.append("")

    out.append("## ExecuTorch programs\n")
    out.append(
        "Each program was run with the ExecuTorch Python runtime on test photos and "
        "compared with the eager model.\n"
    )
    out.append(
        "| File | Backend | Size | Delegated ops | Accuracy (check set) | "
        "Top-1 agreement with eager |\n|---|---|---|---|---|---|"
    )
    for f in man["files"]:
        chk = f.get("check", {})
        acc = pct(chk["accuracy"]) if "accuracy" in chk else "GPU only, checked in browser"
        agree = pct(chk["top1_agreement_with_eager"]) if "accuracy" in chk else "-"
        deleg = (
            "all"
            if f["delegates"] and not f["non_delegated_ops"]
            else ("none (portable)" if not f["delegates"] else f"partial: {f['non_delegated_ops']}")
        )
        out.append(
            f"| `{f['file']}` | {f['target']} | {f['bytes'] / 1e6:.1f} MB | {deleg} "
            f"| {acc} | {agree} |"
        )
    n_check = next(
        (f["check"]["photos"] for f in man["files"] if "photos" in f.get("check", {})), 0
    )
    out.append(f"\nCheck set: {n_check} test photos.\n")

    if bench:
        out.append("## Latency\n")
        out.append(
            f"Measured with `shamba bench` on {bench['machine']} ({bench['cpus']} CPUs), "
            f"median of {bench['runs']} runs, batch 1:\n"
        )
        out.append("| File | Median | p90 |\n|---|---|---|")
        for name, r in bench["models"].items():
            if "median_ms" in r:
                out.append(f"| `{name}` | {r['median_ms']} ms | {r['p90_ms']} ms |")
        out.append("")

    out.append("## How to use\n")
    out.append(
        "Input: `float32 [1, 3, 224, 224]`, RGB in `[0, 1]`, contiguous NCHW. "
        "Take the center square of side `short_side * 224 / 256` and scale it to 224. "
        "Normalization and softmax are inside the program. "
        "Output: probabilities over `classes` in `manifest.json`.\n"
    )
    out.append(
        "```python\nfrom executorch.runtime import Runtime\nimport torch\n\n"
        'method = Runtime.get().load_program("shamba_crop_health_xnnpack_int8.pte")'
        '.load_method("forward")\n'
        "probs = method.execute([torch.rand(1, 3, 224, 224)])[0]\n```\n"
    )
    out.append("Or with the CLI: `shamba predict leaf.jpg --lang sw`.\n")

    out.append("## Training\n")
    out.append(
        f"- Backbone: `{man['backbone']}` (ImageNet-1k weights from timm), full fine-tune.\n"
        f"- Data: {split_text}. Per-class cap of 1,000 photos. "
        "Stratified 80/10/10 split (beans keeps its own split).\n"
        "- Recipe: AdamW, cosine schedule with warm-up, label smoothing 0.1, class-balanced "
        "sampling, random resized crop, flips, rotation, colour jitter, blur, random erasing.\n"
        "- Hardware: 8-core laptop CPU, no GPU.\n"
    )
    if hist:
        out.append("| Epoch | Train loss | Val accuracy | Val macro F1 |\n|---|---|---|---|")
        for h in hist:
            out.append(
                f"| {h['epoch']} | {h['train_loss']:.3f} | {pct(h['val_accuracy'])} "
                f"| {pct(h['val_macro_f1'])} |"
            )
        out.append("")

    out.append("## Data sources\n")
    out.append("| Source | Region | Licence (Hub tag) |\n|---|---|---|")
    for src in cfg.sources:
        out.append(
            f"| [`{src['repo']}`](https://huggingface.co/datasets/{src['repo']}) "
            f"| {src['region']} | {src['license']} |"
        )
    out.append("")

    out.append("## Limits\n")
    out.append(
        "- Only the 14 classes listed. A photo of another crop or disease still gets one of "
        'these labels. The apps show "Not sure" below 45% confidence and ask for a new photo.\n'
        "- Test photos come from the same sources as training photos. Accuracy on photos from "
        "new farms, phones and seasons will be lower. A field pilot must measure it.\n"
        "- The cassava mirror's licence tag is MIT, but the photos come from the Kaggle 2020 "
        "competition (Makerere AI Lab). Check the original terms before commercial use.\n"
        "- This is decision support, not a lab test.\n"
    )
    out.append("## Citation\n")
    out.append("See `CITATION.cff` in the [GitHub repo](https://github.com/ganji759/shamba-ai).\n")
    Path(a.out).write_text("\n".join(out))
    print(f"wrote {a.out}")


if __name__ == "__main__":
    main()
