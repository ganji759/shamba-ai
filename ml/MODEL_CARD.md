---
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
base_model: timm/mobilenetv4_conv_small.e2400_r224_in1k
---

# Shamba crop-health v1 (ExecuTorch)

A small image classifier for leaf diseases of beans, cassava, maize and banana, trained on field photos from Uganda and Tanzania. It runs fully on device with [PyTorch ExecuTorch](https://github.com/pytorch/executorch): Android and laptops (XNNPACK) and web browsers (WebGPU, or the portable CPU kernels in WebAssembly).

It powers [Shamba AI](https://github.com/ganji759/shamba-ai), an offline crop-health assistant for smallholder farmers. Advice for each class is in the app's knowledge base, not in this model.

## Results

Held-out test split, 1226 photos, 14 classes, eager PyTorch model (epoch 9):

| Metric | Value |
|---|---|
| Accuracy | 87.8% |
| Macro F1 | 88.6% |

Per class:

| Class | Precision | Recall | F1 | Photos |
|---|---|---|---|---|
| `bean_angular_leaf_spot` | 95.2% | 93.0% | 94.1% | 43 |
| `bean_rust` | 89.1% | 95.3% | 92.1% | 43 |
| `bean_healthy` | 100.0% | 95.2% | 97.6% | 42 |
| `cassava_bacterial_blight` | 80.0% | 84.0% | 82.0% | 100 |
| `cassava_brown_streak` | 75.8% | 69.0% | 72.3% | 100 |
| `cassava_green_mottle` | 72.8% | 75.0% | 73.9% | 100 |
| `cassava_mosaic` | 80.6% | 75.0% | 77.7% | 100 |
| `cassava_healthy` | 64.8% | 70.0% | 67.3% | 100 |
| `maize_lethal_necrosis` | 95.0% | 95.0% | 95.0% | 100 |
| `maize_streak_virus` | 97.9% | 94.0% | 95.9% | 100 |
| `maize_healthy` | 98.0% | 100.0% | 99.0% | 100 |
| `banana_black_sigatoka` | 99.0% | 100.0% | 99.5% | 100 |
| `banana_fusarium_wilt` | 95.9% | 95.9% | 95.9% | 98 |
| `banana_healthy` | 98.0% | 99.0% | 98.5% | 100 |

## ExecuTorch programs

Each program was run with the ExecuTorch Python runtime on test photos and compared with the eager model.

| File | Backend | Size | Delegated ops | Accuracy (check set) | Top-1 agreement with eager |
|---|---|---|---|---|---|
| `shamba_crop_health_xnnpack_int8.pte` | xnnpack-int8 | 2.7 MB | all | 88.0% | 97.3% |
| `shamba_crop_health_xnnpack.pte` | xnnpack | 10.0 MB | all | 87.7% | 100.0% |
| `shamba_crop_health_webgpu.pte` | webgpu | 10.0 MB | all | GPU only, checked in browser | - |
| `shamba_crop_health_portable.pte` | portable | 10.2 MB | none (portable) | 87.7% | 100.0% |
| `shamba_crop_health_vulkan.pte` | vulkan | 10.0 MB | all | GPU only, checked in browser | - |

Check set: 300 test photos.

## Latency

Measured with `shamba bench` on Intel(R) Core(TM) i5-8250U CPU @ 1.60GHz (8 CPUs), median of 50 runs, batch 1:

| File | Median | p90 |
|---|---|---|
| `shamba_crop_health_xnnpack_int8.pte` | 1.92 ms | 2.5 ms |
| `shamba_crop_health_xnnpack.pte` | 2.9 ms | 3.54 ms |
| `shamba_crop_health_portable.pte` | 1115.8 ms | 1123.63 ms |

## How to use

Input: `float32 [1, 3, 224, 224]`, RGB in `[0, 1]`, contiguous NCHW. Take the center square of side `short_side * 224 / 256` and scale it to 224. Normalization and softmax are inside the program. Output: probabilities over `classes` in `manifest.json`.

```python
from executorch.runtime import Runtime
import torch

method = Runtime.get().load_program("shamba_crop_health_xnnpack_int8.pte").load_method("forward")
probs = method.execute([torch.rand(1, 3, 224, 224)])[0]
```

Or with the CLI: `shamba predict leaf.jpg --lang sw`.

## Training

- Backbone: `mobilenetv4_conv_small.e2400_r224_in1k` (ImageNet-1k weights from timm), full fine-tune.
- Data: 12,280 field photos (9,823 train, 1,231 validation, 1,226 test). Per-class cap of 1,000 photos. Stratified 80/10/10 split (beans keeps its own split).
- Recipe: AdamW, cosine schedule with warm-up, label smoothing 0.1, class-balanced sampling, random resized crop, flips, rotation, colour jitter, blur, random erasing.
- Hardware: 8-core laptop CPU, no GPU.

| Epoch | Train loss | Val accuracy | Val macro F1 |
|---|---|---|---|
| 0 | 1.936 | 65.2% | 66.0% |
| 1 | 1.213 | 76.7% | 77.0% |
| 2 | 1.113 | 76.8% | 78.6% |
| 3 | 1.059 | 82.8% | 84.3% |
| 4 | 0.983 | 82.0% | 83.9% |
| 5 | 0.943 | 82.9% | 84.7% |
| 6 | 0.901 | 85.1% | 86.6% |
| 7 | 0.878 | 85.9% | 87.3% |
| 8 | 0.843 | 87.0% | 88.3% |
| 9 | 0.835 | 87.5% | 88.8% |

## Data sources

| Source | Region | Licence (Hub tag) |
|---|---|---|
| [`AI-Lab-Makerere/beans`](https://huggingface.co/datasets/AI-Lab-Makerere/beans) | Uganda | MIT |
| [`Engineer101/cassava-leaf-disease-classification`](https://huggingface.co/datasets/Engineer101/cassava-leaf-disease-classification) | Uganda | MIT (mirror). Origin: Makerere AI Lab / Kaggle 2020. Check origin terms before commercial use. |
| [`Project-AgML/maize_disease_classification`](https://huggingface.co/datasets/Project-AgML/maize_disease_classification) | Tanzania | CC-BY-4.0 |
| [`Project-AgML/banana_disease_classification_tanzania`](https://huggingface.co/datasets/Project-AgML/banana_disease_classification_tanzania) | Tanzania | CC0-1.0 |

## Limits

- Only the 14 classes listed. A photo of another crop or disease still gets one of these labels. The apps show "Not sure" below 45% confidence and ask for a new photo.
- Test photos come from the same sources as training photos. Accuracy on photos from new farms, phones and seasons will be lower. A field pilot must measure it.
- The cassava mirror's licence tag is MIT, but the photos come from the Kaggle 2020 competition (Makerere AI Lab). Check the original terms before commercial use.
- This is decision support, not a lab test.

## Citation

See `CITATION.cff` in the [GitHub repo](https://github.com/ganji759/shamba-ai).
