"""Build an untrained stand-in model with the real contract, for CI only.

CI jobs for the apps need a .pte and manifest.json with the same shapes and class
list as the release model. Training is too slow for CI, so this script exports a
randomly initialised network. Never ship its output.

    python scripts/make_ci_model.py --out artifacts
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path

import torch

from shamba_ml.config import load_config
from shamba_ml.export import FILE_STEM, _lower, _quantize_int8, _sha256
from shamba_ml.model import DeployableClassifier, create_backbone

BACKBONE = "mobilenetv4_conv_small.e2400_r224_in1k"


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--config", default="configs/crops-v1.yaml")
    ap.add_argument("--out", default="artifacts")
    ap.add_argument("--targets", default="xnnpack-int8,portable")
    ap.add_argument("--force", action="store_true", help="overwrite a real model in --out")
    a = ap.parse_args()

    out = Path(a.out)
    if (out / "manifest.json").exists() and not a.force:
        if not json.loads((out / "manifest.json").read_text()).get("ci_stand_in"):
            raise SystemExit(f"{out} holds a real model. Use --force to overwrite it.")
    out.mkdir(parents=True, exist_ok=True)

    cfg = load_config(a.config)
    torch.manual_seed(0)
    model = DeployableClassifier(
        create_backbone(BACKBONE, cfg.num_classes, pretrained=False)
    ).eval()
    example = (torch.rand(1, 3, cfg.image_size, cfg.image_size),)
    files = []
    for target in a.targets.split(","):
        m = (
            _quantize_int8(model, example, [torch.rand_like(example[0]) for _ in range(4)])
            if target.endswith("int8")
            else model
        )
        buf, n_delegates, non_delegated = _lower(m, example, target)
        name = f"{FILE_STEM}_{target.replace('-', '_')}.pte"
        (out / name).write_bytes(buf)
        files.append(
            {
                "file": name,
                "target": target,
                "bytes": len(buf),
                "sha256": _sha256(buf),
                "delegates": n_delegates,
                "non_delegated_ops": non_delegated,
            }
        )
    manifest = {
        "name": "shamba-crop-health",
        "version": "0.0.0-ci",
        "ci_stand_in": True,
        "config": cfg.name,
        "backbone": BACKBONE,
        "classes": cfg.classes,
        "input": {
            "shape": [1, 3, cfg.image_size, cfg.image_size],
            "dtype": "float32",
            "layout": "NCHW",
            "range": [0.0, 1.0],
            "color": "RGB",
        },
        "output": {"shape": [1, cfg.num_classes], "meaning": "softmax probabilities"},
        "thresholds": {"confident": 0.70, "uncertain_below": 0.45},
        "files": files,
    }
    (out / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
    print(f"CI stand-in model written to {out}")


if __name__ == "__main__":
    main()
