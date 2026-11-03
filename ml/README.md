# shamba-ml

Train, export and run the Shamba crop-health model with PyTorch ExecuTorch. Everything runs on a laptop CPU. A GPU makes training faster but is not needed.

## Setup

```bash
uv venv --python 3.12
uv pip install -e ".[dev]" --extra-index-url https://download.pytorch.org/whl/cpu --index-strategy unsafe-best-match
source .venv/bin/activate
```

Versions used for the v1 release: Python 3.12, torch 2.15 (CPU), executorch 1.5.1, timm 1.0.

## Pipeline

```bash
shamba prepare                       # 1. field photos from the Hugging Face Hub -> data/processed
shamba train --epochs 10             # 2. fine-tune MobileNetV4-conv-small -> runs/v1/best.pt
shamba evaluate --split test         # 3. accuracy, macro F1, confusion matrix -> runs/v1/metrics.json
shamba export --targets xnnpack-int8,xnnpack,webgpu,portable,vulkan   # 4. .pte files -> artifacts/
shamba bench --models artifacts      # 5. latency on this machine
shamba predict leaf.jpg --lang sw    # 6. offline diagnosis with advice
python scripts/model_card.py         # 7. MODEL_CARD.md from the measured JSON files
```

### 1. Prepare

`configs/crops-v1.yaml` lists the sources and maps each source label to a Shamba class id. The script reads only the parquet row groups it needs and stops when every class reaches its cap. Photos are resized (short side 256) and saved as JPEG. The full set is about 380 MB.

| Source | Crop | Region | Licence (Hub tag) |
|---|---|---|---|
| `AI-Lab-Makerere/beans` | beans | Uganda | MIT |
| `Engineer101/cassava-leaf-disease-classification` | cassava | Uganda | MIT (mirror of Kaggle 2020, check origin terms) |
| `Project-AgML/maize_disease_classification` | maize | Tanzania | CC-BY-4.0 |
| `Project-AgML/banana_disease_classification_tanzania` | banana | Tanzania | CC0-1.0 |

To add a crop: add a source and its classes to the config, add advice for each class to `../knowledge/diseases.json`, then run `pytest`. The contract test fails if a class has no advice.

### 2. Train

Full fine-tune of `timm/mobilenetv4_conv_small.e2400_r224_in1k`. AdamW, cosine schedule with warm-up, label smoothing, class-balanced sampling and field-style augmentation (light, blur, rotation, erasing). The best epoch by validation macro F1 is kept.

### 4. Export

`shamba export` wraps the network with normalization and softmax, then lowers it with `torch.export` and `to_edge_transform_and_lower`:

| Target | Partitioner | Notes |
|---|---|---|
| `xnnpack-int8` | `XnnpackPartitioner` | PT2E static int8 (`XNNPACKQuantizer`, per-channel), calibrated on 128 validation photos |
| `xnnpack` | `XnnpackPartitioner` | fp32 |
| `webgpu` | `WebGPUPartitioner` | fp32, for the ExecuTorch Wasm + WebGPU runtime |
| `vulkan` | `VulkanPartitioner` | fp32, Android GPU |
| `portable` | none | browser CPU fallback |

Each CPU program runs on 300 test photos with the ExecuTorch Python runtime. `manifest.json` records size, sha256, delegated ops, accuracy and top-1 agreement with the eager model.

## Tests

```bash
ruff check . && pytest -q
```

`tests/test_contract.py` checks the contract every app depends on: classes match the knowledge base, outputs are probabilities, `.pte` output equals eager output, and non-contiguous input is handled.

## CI stand-in model

`python scripts/make_ci_model.py` writes an untrained model with the real contract. CI uses it when no release model is available. Never ship it.
