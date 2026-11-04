"""Latency of every CPU-runnable .pte in a folder, measured with the ExecuTorch runtime."""

from __future__ import annotations

import json
import os
import platform
import statistics
import time
from pathlib import Path

import torch


def _cpu_name() -> str:
    try:
        for line in Path("/proc/cpuinfo").read_text().splitlines():
            if line.startswith("model name"):
                return line.split(":", 1)[1].strip()
    except OSError:
        pass
    return f"{platform.machine()} {platform.processor()}".strip()


def run(models_dir: str, runs: int = 30) -> dict:
    from executorch.runtime import Runtime

    manifest = json.loads((Path(models_dir) / "manifest.json").read_text())
    x = torch.rand(*manifest["input"]["shape"])
    results = {
        "machine": _cpu_name(),
        "cpus": os.cpu_count(),
        "runs": runs,
        "models": {},
    }
    for f in manifest["files"]:
        path = Path(models_dir) / f["file"]
        try:
            t0 = time.perf_counter()
            method = Runtime.get().load_program(path).load_method("forward")
            load_ms = (time.perf_counter() - t0) * 1000
            for _ in range(3):
                method.execute([x])
            times = []
            for _ in range(runs):
                t = time.perf_counter()
                method.execute([x])
                times.append((time.perf_counter() - t) * 1000)
            results["models"][f["file"]] = {
                "load_ms": round(load_ms, 1),
                "median_ms": round(statistics.median(times), 2),
                "p90_ms": round(sorted(times)[int(0.9 * len(times)) - 1], 2),
                "mb": round(f["bytes"] / 1e6, 2),
            }
        except Exception as e:  # noqa: BLE001
            results["models"][f["file"]] = {"skipped": str(e).splitlines()[0][:120]}
    return results
