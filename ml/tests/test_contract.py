"""The on-device contract: what every app relies on. Fast: no training, no downloads."""

from __future__ import annotations

import json
from pathlib import Path

import numpy as np
import pytest
import torch
from PIL import Image

from shamba_ml.config import load_config
from shamba_ml.export import _lower, _run_pte
from shamba_ml.infer import preprocess
from shamba_ml.metrics import report
from shamba_ml.model import DeployableClassifier, create_backbone

ROOT = Path(__file__).resolve().parents[1]
CONFIG = ROOT / "configs" / "crops-v1.yaml"
KNOWLEDGE = ROOT.parent / "knowledge" / "diseases.json"
BACKBONE = "mobilenetv4_conv_small.e2400_r224_in1k"


@pytest.fixture(scope="module")
def cfg():
    return load_config(CONFIG)


@pytest.fixture(scope="module")
def model(cfg):
    torch.manual_seed(0)
    return DeployableClassifier(create_backbone(BACKBONE, cfg.num_classes, pretrained=False)).eval()


def test_config_classes_match_knowledge_base(cfg):
    kb = json.loads(KNOWLEDGE.read_text())
    assert set(cfg.classes) == set(kb["conditions"]), (
        "every class needs advice, every advice a class"
    )
    for cid, c in kb["conditions"].items():
        for lang in kb["languages"]:
            assert c["name"][lang] and c["actions"][lang], f"{cid} missing {lang}"
        assert c["crop"] == cfg.crop_of(cid)


def test_model_outputs_probabilities(model, cfg):
    with torch.no_grad():
        p = model(torch.rand(2, 3, 224, 224))
    assert p.shape == (2, cfg.num_classes)
    assert torch.allclose(p.sum(-1), torch.ones(2), atol=1e-5)


@pytest.mark.parametrize("target", ["portable", "xnnpack"])
def test_pte_matches_eager(model, target):
    example = (torch.rand(1, 3, 224, 224),)
    buf, _, non_delegated = _lower(model, example, target)
    if target == "xnnpack":
        assert non_delegated == [], "every op must run in the XNNPACK delegate"
    xs = [torch.rand(1, 3, 224, 224) for _ in range(3)]
    got = _run_pte(buf, xs)
    with torch.no_grad():
        want = torch.cat([model(x) for x in xs]).numpy()
    np.testing.assert_allclose(got, want, atol=1e-4)


def test_non_contiguous_input_is_handled(model):
    """ExecuTorch reads raw memory. A strided view must be made contiguous first."""
    buf, _, _ = _lower(model, (torch.rand(1, 3, 224, 224),), "portable")
    x = torch.rand(1, 224, 224, 3).permute(0, 3, 1, 2)  # non-contiguous NCHW view
    assert not x.is_contiguous()
    with torch.no_grad():
        want = model(x).numpy()
    np.testing.assert_allclose(_run_pte(buf, [x]), want, atol=1e-4)


def test_preprocess_shape_and_range(tmp_path):
    img = Image.fromarray((np.random.rand(300, 500, 3) * 255).astype("uint8"))
    path = tmp_path / "leaf.jpg"
    img.save(path)
    x = preprocess(path)
    assert x.shape == (1, 3, 224, 224) and x.is_contiguous()
    assert 0.0 <= float(x.min()) and float(x.max()) <= 1.0


def test_metrics_report():
    r = report([0, 1, 1, 2], [0, 1, 2, 2], ["a", "b", "c"])
    assert r["summary"]["accuracy"] == 0.75
    assert r["confusion_matrix"][1] == [0, 1, 1]
    assert 0 < r["summary"]["macro_f1"] < 1
