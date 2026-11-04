"""Offline diagnosis on a laptop with the ExecuTorch Python runtime.

The same .pte file runs on Android (Kotlin), in the browser (Wasm) and here.
"""

from __future__ import annotations

import json
import time
from pathlib import Path

import numpy as np
import torch
from PIL import Image, ImageOps


def preprocess(path: str | Path, size: int = 224, short_side: int = 256) -> torch.Tensor:
    """Resize short side, center crop, scale to [0, 1]. Matches the app code."""
    img = ImageOps.exif_transpose(Image.open(path)).convert("RGB")
    w, h = img.size
    s = short_side / min(w, h)
    img = img.resize((max(size, round(w * s)), max(size, round(h * s))), Image.BILINEAR)
    w, h = img.size
    left, top = (w - size) // 2, (h - size) // 2
    img = img.crop((left, top, left + size, top + size))
    arr = np.asarray(img, dtype=np.float32) / 255.0
    return torch.from_numpy(arr.transpose(2, 0, 1).copy()).unsqueeze(0)


class Diagnoser:
    def __init__(self, model: str, manifest: str, knowledge: str | None = None):
        from executorch.runtime import Runtime

        self.manifest = json.loads(Path(manifest).read_text())
        self.method = Runtime.get().load_program(Path(model)).load_method("forward")
        self.kb = (
            json.loads(Path(knowledge).read_text())
            if knowledge and Path(knowledge).exists()
            else None
        )

    def probabilities(self, image: str | Path) -> tuple[np.ndarray, float]:
        size = self.manifest["input"]["shape"][-1]
        x = preprocess(image, size)
        t0 = time.perf_counter()
        probs = self.method.execute([x])[0].numpy()[0].copy()
        return probs, (time.perf_counter() - t0) * 1000

    def diagnose(self, image: str | Path, lang: str = "en", top_k: int = 3) -> dict:
        probs, ms = self.probabilities(image)
        classes = self.manifest["classes"]
        order = np.argsort(-probs)[:top_k]
        best = classes[order[0]]
        conf = float(probs[order[0]])
        th = self.manifest["thresholds"]
        status = (
            "confident"
            if conf >= th["confident"]
            else "uncertain"
            if conf < th["uncertain_below"]
            else "likely"
        )
        result = {
            "status": status,
            "top": [{"class": classes[i], "probability": round(float(probs[i]), 4)} for i in order],
            "latency_ms": round(ms, 1),
        }
        if self.kb and status != "uncertain":
            entry = self.kb["conditions"].get(best, {})
            result["advice"] = {
                k: _pick(v, lang)
                for k, v in entry.items()
                if k in ("name", "summary", "actions", "prevention", "urgency", "escalate")
            }
        if status == "uncertain":
            result["advice"] = {
                "retake": _pick(self.kb["ui"]["retake_tips"], lang)
                if self.kb
                else "Take a closer photo of one leaf in daylight."
            }
        return result


def _pick(value, lang: str):
    """Knowledge base fields are {lang: text}. Fall back to English."""
    if isinstance(value, dict) and "en" in value:
        return value.get(lang) or value["en"]
    return value
