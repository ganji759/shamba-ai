"""Export the trained model to ExecuTorch programs for every Shamba client.

Targets:
    xnnpack       fp32, CPU (Android, laptops, Raspberry Pi)
    xnnpack-int8  PT2E static int8 with calibration, CPU. Default for phones.
    webgpu        fp32, GPU in the browser (ExecuTorch Wasm + WebGPU runtime)
    vulkan        fp32, GPU on Android
    portable      no delegate. Browser fallback when WebGPU is missing.

Every program is checked against the eager model on held-out photos, and the
results go into manifest.json. Apps read manifest.json to pick a file and to
verify its sha256 after download.
"""

from __future__ import annotations

import hashlib
import json
import platform
import time
from pathlib import Path

import numpy as np
import torch

from shamba_ml.data.dataset import ManifestDataset, eval_transform
from shamba_ml.model import DeployableClassifier, load_checkpoint

FILE_STEM = "shamba_crop_health"


def _partitioner(target: str):
    if target.startswith("xnnpack"):
        from executorch.backends.xnnpack.partition.xnnpack_partitioner import XnnpackPartitioner

        return [XnnpackPartitioner()]
    if target == "webgpu":
        from executorch.backends.webgpu.partitioner import WebGPUPartitioner

        return [WebGPUPartitioner()]
    if target == "vulkan":
        from executorch.backends.vulkan.partitioner.vulkan_partitioner import VulkanPartitioner

        return [VulkanPartitioner()]
    if target == "portable":
        return []
    raise ValueError(f"unknown target {target}")


def _quantize_int8(model: torch.nn.Module, example, calib: list[torch.Tensor]) -> torch.nn.Module:
    from executorch.backends.xnnpack.quantizer.xnnpack_quantizer import (
        XNNPACKQuantizer,
        get_symmetric_quantization_config,
    )
    from torchao.quantization.pt2e.quantize_pt2e import convert_pt2e, prepare_pt2e

    q = XNNPACKQuantizer()
    q.set_global(get_symmetric_quantization_config(is_per_channel=True))
    m = prepare_pt2e(torch.export.export(model, example).module(), q)
    with torch.no_grad():
        for x in calib:
            m(x)
    return convert_pt2e(m)


def _lower(model: torch.nn.Module, example, target: str) -> bytes:
    from executorch.exir import to_edge_transform_and_lower

    ep = torch.export.export(model, example)
    prog = to_edge_transform_and_lower(ep, partitioner=_partitioner(target)).to_executorch()
    plan = prog.executorch_program.execution_plan[0]
    non_delegated = sorted({o.name for o in plan.operators})
    print(f"[{target}] delegates={len(plan.delegates)} non-delegated ops={non_delegated}")
    return prog.buffer, len(plan.delegates), non_delegated


def _run_pte(buf: bytes, inputs: list[torch.Tensor]) -> np.ndarray:
    from executorch.runtime import Runtime

    method = Runtime.get().load_program(buf).load_method("forward")
    # The runtime reuses its output buffer: copy each result before the next call.
    return np.concatenate([method.execute([x.contiguous()])[0].numpy().copy() for x in inputs])


def _sha256(b: bytes) -> str:
    return hashlib.sha256(b).hexdigest()


def run(
    checkpoint: str,
    out_dir: str,
    targets: list[str],
    calib_data: str,
    n_calib: int = 128,
    n_check: int = 300,
) -> Path:
    import executorch
    from executorch.version import git_version

    out = Path(out_dir)
    out.mkdir(parents=True, exist_ok=True)
    backbone, ckpt = load_checkpoint(checkpoint)
    model = DeployableClassifier(backbone, ckpt["mean"], ckpt["std"]).eval()
    size = ckpt["image_size"]
    example = (torch.rand(1, 3, size, size),)

    # Held-out photos, preprocessed exactly as the apps do (no normalization: it is in-graph).
    tf = eval_transform(size, ckpt["resize_short_side"], normalize=False)
    calib_ds = ManifestDataset(calib_data, "val", ckpt["classes"], tf, limit=n_calib)
    check_ds = ManifestDataset(calib_data, "test", ckpt["classes"], tf, limit=n_check)
    calib = [calib_ds[i][0].unsqueeze(0) for i in range(len(calib_ds))]
    check = [check_ds[i][0].unsqueeze(0) for i in range(len(check_ds))]
    labels = np.array([check_ds[i][1] for i in range(len(check_ds))])
    with torch.no_grad():
        eager = torch.cat([model(x) for x in check]).numpy()
    eager_acc = float((eager.argmax(1) == labels).mean())
    print(f"eager accuracy on {len(check)} test photos: {eager_acc:.4f}")

    files = []
    for target in targets:
        t0 = time.time()
        m = _quantize_int8(model, example, calib) if target.endswith("int8") else model
        buf, n_delegates, non_delegated = _lower(m, example, target)
        name = f"{FILE_STEM}_{target.replace('-', '_')}.pte"
        (out / name).write_bytes(buf)
        entry = {
            "file": name,
            "target": target,
            "bytes": len(buf),
            "sha256": _sha256(buf),
            "dtype": "int8" if target.endswith("int8") else "float32",
            "delegates": n_delegates,
            "non_delegated_ops": non_delegated,
            "export_seconds": round(time.time() - t0, 1),
        }
        try:
            probs = _run_pte(buf, check)
            entry["check"] = {
                "runtime": "executorch.runtime (Python, CPU)",
                "photos": len(check),
                "accuracy": float((probs.argmax(1) == labels).mean()),
                "top1_agreement_with_eager": float((probs.argmax(1) == eager.argmax(1)).mean()),
                "max_abs_prob_diff": float(np.abs(probs - eager).max()),
            }
        except Exception as e:  # noqa: BLE001 - GPU programs cannot run on CPU pybindings
            entry["check"] = {
                "runtime": "not runnable on CPU",
                "reason": str(e).splitlines()[0][:200],
            }
        print(json.dumps(entry))
        files.append(entry)

    manifest = {
        "name": "shamba-crop-health",
        "version": "1.0.0",
        "config": ckpt["config_name"],
        "backbone": ckpt["backbone"],
        "classes": ckpt["classes"],
        "input": {
            "shape": [1, 3, size, size],
            "dtype": "float32",
            "layout": "NCHW",
            "range": [0.0, 1.0],
            "color": "RGB",
            "preprocess": f"resize short side to {ckpt['resize_short_side']} (bilinear), "
            f"center crop {size}x{size}, divide by 255",
        },
        "output": {"shape": [1, len(ckpt["classes"])], "meaning": "softmax probabilities"},
        "thresholds": {"confident": 0.70, "uncertain_below": 0.45},
        "eval": {"eager_accuracy_on_check_set": eager_acc, "val": ckpt["val"]},
        "files": files,
        "built_with": {
            "executorch": executorch.version.__version__,
            "executorch_git": git_version,
            "torch": torch.__version__,
            "python": platform.python_version(),
        },
    }
    (out / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
    print(f"wrote {out / 'manifest.json'}")
    return out / "manifest.json"
