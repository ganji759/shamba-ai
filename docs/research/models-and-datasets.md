# Shamba AI v2: models, datasets and prior art

Research date: 2026-11-04. Hub facts (existence, license tag, params, size, download counts) were read from live Hugging Face repo pages on this date. Anything not confirmed is marked **unverified**. A license shown here is the Hub tag, not legal advice. Check the original source before shipping.

## Summary and top recommendations

| Need | Recommendation | Why |
|---|---|---|
| (a) Classifier training set | Merge East African field sets: Kaggle/Makerere cassava (21,367 images, Uganda), `AI-Lab-Makerere/beans`, `Project-AgML/banana_disease_classification_tanzania`, `Project-AgML/banana_leaf_disease_classification`, `Project-AgML/arabica_coffee_leaf_disease_classification`, KaraAgro maize (Dataverse). Use PlantVillage (`mohanty/PlantVillage`) only for pre-training or tomato/potato, not as the main set. | Field images from the target region. PlantVillage is lab-style and transfers poorly to field photos (known from the literature, not re-measured here). |
| (b) Backbone | `timm/mobilenetv3_large_100.ra_in1k` (5.5M params, Apache-2.0). Second choice: `timm/efficientnet_b0.ra_in1k` (5.3M, Apache-2.0). | Both are in timm, so `torch.export` works with the XNNPACK recipe. EfficientNet is also on the optimum-executorch vision list. Accuracy on our data is unverified until we fine-tune. |
| (c) On-device LLM | `Qwen/Qwen3-1.7B` (Apache-2.0) exported with optimum-executorch, 8da4w. Fallback: `Qwen/Qwen3-0.6B`. Use retrieval (RAG) over an English/Swahili knowledge base so the model does not rely on its own crop knowledge. | Qwen3 is on the supported list and has a ready WebGPU .pte for 0.6B. Swahili quality of every small model here is **unverified**. Test on `masakhane/uhura-arc-easy` and our own questions. |
| (d) ASR | Fine-tune `openai/whisper-base` or `whisper-small` (Apache-2.0) on `badrex/swahili-speech-400hr` (CC-BY-4.0) and export through optimum-executorch. Baseline now: `software-mansion/whisper-base-ExecuTorch`. | Whisper is on the supported list. Ready .pte files exist. Stock tiny/base Swahili accuracy is **unverified** and expected to be weak. |

Biggest risks: (1) dataset license gaps (Kaggle cassava, KaraAgro), (2) CC-BY-NC speech models (MMS), (3) Gemma and Llama custom licenses, (4) accuracy drop from lab images to field photos, (5) no verified Swahili quality numbers for sub-2B LLMs.

---

## 1. Image datasets (crop disease and pests)

"Parquet" means the repo exposes parquet and loads with `datasets.load_dataset("<id>")`. Row counts are from the repo card or dataset viewer.

| Dataset | Source / repo id | Images | Classes | Field vs lab | License | Parquet via `load_dataset` |
|---|---|---|---|---|---|---|
| Cassava Leaf Disease (Kaggle, Makerere AI Lab and NaCRRI) | Kaggle competition `cassava-leaf-disease-classification` (not on Kaggle in this session, figures from third-party repos) | 21,367 (unverified, third-party) | 5: CBB, CBSD, CGM, CMD, healthy | Field, Uganda survey | **Unverified** (check Kaggle page) | No on Kaggle. HF mirrors below. |
| iCassava 2019 | [arXiv 1908.02900](https://arxiv.org/abs/1908.02900), [FGVC6 page](https://sites.google.com/view/fgvc6/competitions/icassava-2019) | 9,436 labeled + 12,595 unlabeled (search summary) | 5 | Field, Uganda | Unverified | No |
| Cassava mirror | [`andrewkatumba/cassava_leaf_diseases_dsa_2023`](https://huggingface.co/datasets/andrewkatumba/cassava_leaf_diseases_dsa_2023) | 1,050 (900 train, 150 test) | not shown on card | Likely field (unverified) | CC-BY-SA-4.0 | Yes, parquet, 2.4 GB |
| Cassava mirror | [`Engineer101/cassava-leaf-disease-classification`](https://huggingface.co/datasets/Engineer101/cassava-leaf-disease-classification) | 10K-100K (tag) | likely 5 | Field | MIT tag, provenance unclear | imagefolder, not parquet |
| Cassava whitefly (detection) | [`Project-AgML/Cassava_whitefly_detection`](https://huggingface.co/datasets/Project-AgML/Cassava_whitefly_detection) | 3,000 images, 212,951 boxes | 1 (whitefly) | Field, Namulonge Uganda, phone photos | not stated on card | Yes, parquet |
| Beans (iBean) | [`AI-Lab-Makerere/beans`](https://huggingface.co/datasets/AI-Lab-Makerere/beans) | 1,295 (1,034 / 133 / 128) | 3: angular leaf spot, bean rust, healthy | Field (card says only "Makerere AI Lab", provenance blank) | MIT | Yes, parquet, 519K downloads |
| Banana Tanzania (NM-AIST) | [`Project-AgML/banana_disease_classification_tanzania`](https://huggingface.co/datasets/Project-AgML/banana_disease_classification_tanzania) | 16,092 | 3: black sigatoka 5,767, fusarium wilt 4,697, healthy 5,628 | Not stated (Tanzania, Mduma and Leo 2023, Data in Brief 49:109322) | CC0-1.0 | Yes, parquet |
| Banana Sigatoka/Xanthomonas | [`Project-AgML/banana_leaf_disease_classification`](https://huggingface.co/datasets/Project-AgML/banana_leaf_disease_classification) | 1,288 | 3: healthy 154, sigatoka 320, xanthomonas 814 | Unverified | CC-BY-4.0 | Yes, parquet |
| PFSD Musa banana | [`Project-AgML/PFSD_Musa_banana_disease_classification`](https://huggingface.co/datasets/Project-AgML/PFSD_Musa_banana_disease_classification) | 6,700 | 8 incl. bacterial soft rot, aphids, black/yellow sigatoka, Panama disease, pseudostem weevil, K deficiency | Unverified | CC-BY-4.0 | Yes, parquet |
| Arabica coffee leaf | [`Project-AgML/arabica_coffee_leaf_disease_classification`](https://huggingface.co/datasets/Project-AgML/arabica_coffee_leaf_disease_classification) | 58,549 (likely augmented, unverified) | 5: cercospora 7,681, healthy 18,983, rust 8,336, miner 16,978, phoma 6,571 | Unverified | CC-BY-4.0 | Yes, parquet |
| Maize (KaraAgro AI, Lacuna Fund) | Harvard Dataverse "KaraAgro AI Maize" (see [datasetninja](https://datasetninja.com/kara-agro-ai-maize)) | 16,552 (third-party catalog) | healthy, fall armyworm, maize streak virus | Field, Ghana | Lacuna default CC-BY-4.0, record itself **unverified** | No (Dataverse download) |
| Maize community uploads | [`DaveTheGreat/maize-disease-dataset`](https://huggingface.co/datasets/DaveTheGreat/maize-disease-dataset), [`muAtarist/maize_foliar_diseases`](https://huggingface.co/datasets/muAtarist/maize_foliar_diseases), [`Spitblaze/Corn_or_Maize_Leaf_Disease_Dataset`](https://huggingface.co/datasets/Spitblaze/Corn_or_Maize_Leaf_Disease_Dataset) | 1K-100K (tags) | unverified | unverified | none / unknown | imagefolder, not parquet |
| Lacuna: crop pest and disease imagery and spectrometry | [Lacuna agriculture page](https://lacunafund.org/datasets/agriculture/) (Makerere AI Lab, NM-AIST, KaraAgro AI, NUST) | about 140,000 planned across 7 crops (search summary) | about 20 diseases and pests | Field | Lacuna default CC-BY-4.0 unless noted | No. Page gives no links, crops or sizes. Banana, bean, cassava coverage unconfirmed. |
| Eyes on the Ground (One Acre Fund, Lacuna) | [Lacuna agriculture page](https://lacunafund.org/datasets/agriculture/) | not stated | crop damage labels, Kenya (8 counties) | Field | Unverified | No |
| PlantVillage | [`mohanty/PlantVillage`](https://huggingface.co/datasets/mohanty/PlantVillage) | 54,306 | 14 crops, 26 diseases (38 classes with healthy) | Lab (leaf on plain background) | CC-BY-SA-3.0 (share-alike) | Repo format tagged `text`, not parquet. Use parquet mirror below. |
| PlantVillage mirror | [`BrandonFors/Plant-Diseases-PlantVillage-Dataset`](https://huggingface.co/datasets/BrandonFors/Plant-Diseases-PlantVillage-Dataset) | 10K-100K | 38 | Lab | CC0-1.0 tag (upstream is CC-BY-SA-3.0, so this tag looks wrong) | Yes, parquet |
| New Plant Diseases (augmented PlantVillage) | [`Hemg/new-plant-diseases-dataset`](https://huggingface.co/datasets/Hemg/new-plant-diseases-dataset) | 70.3K | 38, includes tomato, potato, maize (gray leaf spot, common rust, northern leaf blight) | Lab, augmented | none given | Yes, parquet, 1.1 GB |
| PlantDoc (detection) | [`agyaatcoder/PlantDoc`](https://huggingface.co/datasets/agyaatcoder/PlantDoc), [`susnato/plant_disease_detection_processed`](https://huggingface.co/datasets/susnato/plant_disease_detection_processed) | 1K-10K (tag; PlantDoc paper says 2,598 images, 27 classes, unverified here) | 27 (unverified) | Field, web-scraped | CC-BY-4.0 | Yes, parquet, object-detection format |
| Multi-crop gated set | [`Saon110/bd-crop-vegetable-plant-disease-dataset`](https://huggingface.co/datasets/Saon110/bd-crop-vegetable-plant-disease-dataset) | 123,588 | 94, 13 crops (Bangladesh) | Unverified | CC-BY-NC-SA-4.0, gated | Yes, parquet |
| Vegetable leaf set | [`Project-AgML/plant_leaf_disease_classification`](https://huggingface.co/datasets/Project-AgML/plant_leaf_disease_classification) | 12,786 | 16 (tomato, eggplant, gourds, etc.) | Unverified | CC-BY-4.0 | Yes, parquet |
| Coffee (student upload) | [`brainer-fp66/coffee-leaf-diseases`](https://huggingface.co/datasets/brainer-fp66/coffee-leaf-diseases) | 1K-10K | 5: healthy, miner, phoma, red spider mite, rust | Unverified | "academic use", not an open license | Yes, parquet |
| Image Safari cassava | [`Project-AgML/image_safari_cassava_qed_lilongwe`](https://huggingface.co/datasets/Project-AgML/image_safari_cassava_qed_lilongwe) | 10K-100K | unlabeled | Field, Malawi, 2025 | CC-BY-SA-4.0 | Yes, parquet |

Searches that returned nothing on the Hub: fall armyworm images, maize lethal necrosis (MLN), potato late blight with East African origin, dedicated Rwandan or DRC crop sets. The KaraAgro and Lacuna fall armyworm images must come from Dataverse. Treat MLN and fall armyworm as **no open labeled set found**.

Notes
- Prefer the Project-AgML mirrors. They have parquet, a card with per-class counts, and a license tag.
- The 58,549 coffee and 16,092 banana counts probably include augmented copies. Deduplicate and split by source image before training. Unverified.
- Verify the Kaggle cassava license before using it in a product. The Hub mirrors carry their own tags that do not prove the original terms.

---

## 2. Pretrained image classifiers (phone-sized)

### 2a. ImageNet backbones to fine-tune (all verified on the Hub)

| Repo id | Architecture | Params | License | timm / torch.export |
|---|---|---|---|---|
| [`timm/mobilenetv3_small_100.lamb_in1k`](https://huggingface.co/timm/mobilenetv3_small_100.lamb_in1k) | MobileNetV3-Small | 2.6M | Apache-2.0 | timm, easy |
| [`timm/mobilenetv3_large_100.ra_in1k`](https://huggingface.co/timm/mobilenetv3_large_100.ra_in1k) | MobileNetV3-Large | 5.5M | Apache-2.0 | timm, easy |
| [`timm/efficientnet_b0.ra_in1k`](https://huggingface.co/timm/efficientnet_b0.ra_in1k) | EfficientNet-B0 | 5.3M | Apache-2.0 | timm, easy. EfficientNet is on the optimum-executorch list. |
| [`timm/tf_efficientnetv2_b0.in1k`](https://huggingface.co/timm/tf_efficientnetv2_b0.in1k) | EfficientNetV2-B0 | 7.2M | Apache-2.0 | timm |
| [`timm/convnext_atto.d2_in1k`](https://huggingface.co/timm/convnext_atto.d2_in1k) | ConvNeXt-atto | 3.7M | Apache-2.0 | timm |
| [`timm/vit_tiny_patch16_224.augreg_in21k_ft_in1k`](https://huggingface.co/timm/vit_tiny_patch16_224.augreg_in21k_ft_in1k) | ViT-Tiny | 5.7M | Apache-2.0 | timm |
| [`timm/efficientformerv2_s0.snap_dist_in1k`](https://huggingface.co/timm/efficientformerv2_s0.snap_dist_in1k) | EfficientFormerV2-S0 | 3.6M | Apache-2.0 | timm |
| [`timm/mobilevitv2_050.cvnets_in1k`](https://huggingface.co/timm/mobilevitv2_050.cvnets_in1k) | MobileViTv2-0.5 | 1.4M | "other" (Apple license, read before use) | timm |
| [`apple/mobilevit-small`](https://huggingface.co/apple/mobilevit-small) | MobileViT-S | not shown | "other" (Apple license) | transformers, on the optimum-executorch vision list |

torchvision also ships MobileNetV3 and EfficientNet with the same architectures. Weights license follows torchvision terms (unverified here).

### 2b. Already fine-tuned plant disease models (verified to exist; accuracy not verified)

None of these is trained on East African crops. Use them only as a sanity baseline. I did not find a reported accuracy with a trustworthy source on the pages checked, so accuracy is **unverified** for all of them.

| Repo id | Architecture | Data | License | Compatibility |
|---|---|---|---|---|
| [`linkanjarad/mobilenet_v2_1.0_224-plant-disease-identification`](https://huggingface.co/linkanjarad/mobilenet_v2_1.0_224-plant-disease-identification) | MobileNetV2 | `image_folder` (probably PlantVillage, unverified) | "other" | transformers, 115K downloads |
| [`onnx-community/mobilenet_v2_1.0_224-plant-disease-identification-ONNX`](https://huggingface.co/onnx-community/mobilenet_v2_1.0_224-plant-disease-identification-ONNX) | same, ONNX | same | "other" | ONNX |
| [`RononoaIgor98/cassava-leaf-disease-efficientnet-b0`](https://huggingface.co/RononoaIgor98/cassava-leaf-disease-efficientnet-b0) | EfficientNet-B0 | cassava (Kaggle, likely) | Apache-2.0 | timm, easy to export. Created Aug 2026, 54 downloads. |
| [`rodynaemad/plant-disease-dinov2-small`](https://huggingface.co/rodynaemad/plant-disease-dinov2-small) | DINOv2-small | `mohanty/PlantVillage` | CC-BY-SA-4.0 | transformers, 22.1M params |
| [`Kathir56/plant-disease-tamilnadu`](https://huggingface.co/Kathir56/plant-disease-tamilnadu) | MobileNetV2 | PlantVillage mirror | Apache-2.0 | safetensors |
| [`mesabo/agri-plant-disease-resnet50`](https://huggingface.co/mesabo/agri-plant-disease-resnet50) | ResNet-50 (too big for phone use) | PlantVillage | Apache-2.0 | transformers |
| [`louislu9911/convnextv2-tiny-1k-224-finetuned-cassava-leaf-disease`](https://huggingface.co/louislu9911/convnextv2-tiny-1k-224-finetuned-cassava-leaf-disease) | ConvNeXtV2-tiny | cassava (imagefolder) | Apache-2.0 | transformers |
| [`as-cle-bert/beit-banana-diseases`](https://huggingface.co/as-cle-bert/beit-banana-diseases) | BEiT (large for phones) | `as-cle-bert/banana-disease-classification` | none | transformers |
| [`Arko007/agromind-plant-disease-nfnet`](https://huggingface.co/Arko007/agromind-plant-disease-nfnet) | NFNet (large) | unverified | Apache-2.0 | timm |

Conclusion: train our own head on the section 1 merge. No published checkpoint covers cassava + beans + banana + coffee + maize together.

---

## 3. Small language models for on-device advisory chat

### 3a. optimum-executorch (from the [README](https://github.com/huggingface/optimum-executorch))

Supported architectures listed there:
- Decoder-only: Codegen, Gemma, Gemma2, **Gemma3**, Glm, Gpt2, GptJ, GptNeoX, GptNeoXJapanese, Granite, **Llama**, Mistral, Olmo, Phi, **Phi4**, Qwen2, **Qwen3**, Smollm, **Smollm3**, Starcoder2
- Encoder-decoder: T5. Encoder-only: Albert, Bert, Distilbert, Eurobert, Roberta
- Vision: Cvt, Deit, Dit, **EfficientNet**, Focalnet, **Mobilevit**, Mobilevit2, Pvt, Swin
- Audio: **Whisper**, Granite Speech, Voxtral
- **Gemma 3n is not on the list.** Treat it as unsupported until confirmed.
- The README says the list keeps growing. Run `optimum-cli export executorch --help` for the current options.

Export command from the README (SmolLM2 example, swap the model id):

```bash
optimum-cli export executorch \
    --model "Qwen/Qwen3-1.7B" \
    --task "text-generation" \
    --recipe "xnnpack" \
    --use_custom_sdpa \
    --use_custom_kv_cache \
    --qlinear 8da4w \
    --qembedding 8w \
    --output_dir="hf_qwen3_1p7b"
```

Python equivalent uses `recipe="xnnpack"`, `attn_implementation="custom_sdpa"`, `use_custom_kv_cache=True`, `qlinear`, `qembedding`. The README says custom SDPA gives about 3x and custom KV cache about 2.5x speedup (README claim, not re-measured). Quantization is TorchAO post-training. Only the model id Qwen3-1.7B is my substitution, I did not run the export.

### 3b. Candidate models

| Repo id | Params (Hub) | License | On supported list | Swahili / French / Kinyarwanda quality |
|---|---|---|---|---|
| [`Qwen/Qwen3-0.6B`](https://huggingface.co/Qwen/Qwen3-0.6B) | 751.6M (includes embeddings) | Apache-2.0 | Yes (Qwen3) | Unverified for all three. Qwen3 report is [arXiv 2505.09388](https://arxiv.org/abs/2505.09388). I did not check its language list. |
| [`Qwen/Qwen3-1.7B`](https://huggingface.co/Qwen/Qwen3-1.7B) | 2,031.7M | Apache-2.0 | Yes | Unverified |
| [`google/gemma-3-270m-it`](https://huggingface.co/google/gemma-3-270m-it) | 268.1M | Gemma terms, gated | Yes (Gemma3) | Unverified, expected weak at this size |
| [`google/gemma-3-1b-it`](https://huggingface.co/google/gemma-3-1b-it) | 999.9M | Gemma terms, gated | Yes | Unverified |
| [`google/gemma-3n-E2B-it`](https://huggingface.co/google/gemma-3n-E2B-it) | 5,439M raw (2B effective claimed) | Gemma terms, gated | **No** | Unverified. Multimodal incl. audio. |
| [`HuggingFaceTB/SmolLM3-3B`](https://huggingface.co/HuggingFaceTB/SmolLM3-3B) | 3,075M | Apache-2.0 | Yes (Smollm3) | Card lists en, fr, es, it, pt, zh, ar, ru. **No Swahili or Kinyarwanda.** French is covered. |
| [`meta-llama/Llama-3.2-1B-Instruct`](https://huggingface.co/meta-llama/Llama-3.2-1B-Instruct) | 1,235.8M | Llama 3.2 community license, gated | Yes | Official languages en, de, fr, it, pt, hi, es, th. No Swahili or Kinyarwanda. |
| [`microsoft/Phi-4-mini-instruct`](https://huggingface.co/microsoft/Phi-4-mini-instruct) | 3,836M | MIT | Yes (Phi4) | Card lists fr, no Swahili or Kinyarwanda. Large for phones. |

### 3c. African-language models

| Repo id | What | Size | License | Notes |
|---|---|---|---|---|
| [`CraneAILabs/swahili-gemma-1b`](https://huggingface.co/CraneAILabs/swahili-gemma-1b) | Gemma 3 1B fine-tuned for Swahili chat and EN to SW translation | 999.9M | Gemma terms | Card reports FLORES-200 EN to SW zero-shot: 27.6 BLEU, 56.8 chrF++ (Gemma 3 4B: 10.9 / 44.1; Gemma 3 27B: 29.4 / 60.0). Output is Swahili only. Training data not disclosed. Context 4,096 tokens. Gemma3 is exportable, so this is a candidate to try with optimum-executorch (not tested). Also exists as [`CraneAILabs/swahili-gemma-1b-litert`](https://huggingface.co/CraneAILabs/swahili-gemma-1b-litert) (LiteRT, not ExecuTorch). |
| [`ngusadeep/gemma-3-270M-Swahili-llm`](https://huggingface.co/ngusadeep/gemma-3-270M-Swahili-llm) | Gemma 3 270M LoRA on `alfaxadeyembe/swahili-instructions` | 270M | tags show no license | Individual project, quality unverified |
| [`Alfaxad/gemma2-2b-swahili-it`](https://huggingface.co/Alfaxad/gemma2-2b-swahili-it) | Gemma 2 2B Swahili | 2B | Apache-2.0 tag | Base model is Gemma 2, which has its own terms |
| [`Jacaranda/UlizaLlama`](https://huggingface.co/Jacaranda/UlizaLlama) | Llama-based Swahili/English QA | not shown (Llama, likely 7B) | none shown | 71K downloads, 2023. Too big for phone. |
| [`Sunbird/Sunflower-32B`](https://huggingface.co/Sunbird/Sunflower-32B) | Qwen3-based, Ugandan languages incl. Kinyarwanda (`kin`) | 32B | not checked | Teacher or translation back-end only, not on-device |
| [`masakhane/zephyr-7b-gemma-sft-african-alpaca`](https://huggingface.co/masakhane/zephyr-7b-gemma-sft-african-alpaca) | Gemma-7B SFT on African Alpaca | 7B | Gemma terms | Too big. Dataset `masakhane/african-translated-alpaca` is a possible SFT source. |
| [`Sunbird/translate-nllb-1.3b-salt`](https://huggingface.co/Sunbird/translate-nllb-1.3b-salt) | NLLB MT for Ugandan languages | 1.3B | no license shown | Alternative path: keep KB in English, translate. T5-style encoder-decoders are on the optimum-executorch list but NLLB (M2M100) is not. |
| Kinyarwanda small LLM | Searched `kinyarwanda LLM instruct`: no result | | | **None found.** |

Evaluation sets to measure our own candidates: [`masakhane/uhura-arc-easy`](https://huggingface.co/datasets/masakhane/uhura-arc-easy) (MIT, sw), [`masakhane/afrimmlu`](https://huggingface.co/datasets/masakhane/afrimmlu), [`masakhane/afrimgsm`](https://huggingface.co/datasets/masakhane/afrimgsm) (both include `kin` and `sw`).

### 3d. Ready-made .pte or ExecuTorch artifacts

- `pytorch` org: search for executorch, pte and Qwen3 returned **no results** through the Hub search tool. Not confirmed either way. Check the org page by hand.
- `executorch-community` org (verified, 20 repos found):
  - Llama: [`Llama-3.2-1B-Instruct-ET`](https://huggingface.co/executorch-community/Llama-3.2-1B-Instruct-ET), [`Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8-ET`](https://huggingface.co/executorch-community/Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8-ET), [`...QLORA_INT4_EO8-ET`](https://huggingface.co/executorch-community/Llama-3.2-1B-Instruct-QLORA_INT4_EO8-ET) (all Llama 3.2 license)
  - [`SmolLM2-135M`](https://huggingface.co/executorch-community/SmolLM2-135M) (Apache-2.0)
  - WebGPU builds (browser, not Android XNNPACK): [`Qwen3-0.6B-ExecuTorch-WebGPU`](https://huggingface.co/executorch-community/Qwen3-0.6B-ExecuTorch-WebGPU), [`Gemma-3-1B-IT-ExecuTorch-WebGPU`](https://huggingface.co/executorch-community/Gemma-3-1B-IT-ExecuTorch-WebGPU), `Qwen2.5-0.5B-Instruct`, `Llama-3.2-1B-Instruct`, `SmolVLM-500M-Instruct`, `Whisper-Base`, `Phonon-31M-EN` (English TTS).
  - Not found: any XNNPACK .pte for Qwen3 or Gemma 3. We must export ourselves.
- Vision .pte for plant disease: **none found** (search with executorch filter returned nothing).

---

## 4. Speech: ASR and TTS

### 4a. ASR

| Repo id | What | Size | License | Notes |
|---|---|---|---|---|
| [`openai/whisper-tiny`](https://huggingface.co/openai/whisper-tiny) | multilingual, `sw` in language tags | 37.8M | Apache-2.0 | Swahili WER unverified, expected high |
| [`openai/whisper-base`](https://huggingface.co/openai/whisper-base) | multilingual | 72.6M | Apache-2.0 | same caveat |
| [`software-mansion/whisper-tiny-ExecuTorch`](https://huggingface.co/software-mansion/whisper-tiny-ExecuTorch) | ready .pte (multilingual), 1.5M downloads | tiny | Apache-2.0 | Also `whisper-base-`, `whisper-small-ExecuTorch`, and `.en` variants (English only, useless for us) |
| [`larryliu0820/whisper-small-INT8-INT4-ExecuTorch-XNNPACK`](https://huggingface.co/larryliu0820/whisper-small-INT8-INT4-ExecuTorch-XNNPACK) | XNNPACK .pte, quantized | small | Apache-2.0 | Also tiny, medium, large-v3-turbo. Individual account, Feb 2026. |
| [`executorch-community/Whisper-Base-ExecuTorch-WebGPU`](https://huggingface.co/executorch-community/Whisper-Base-ExecuTorch-WebGPU) | WebGPU, browser | base | Apache-2.0 | not for Android XNNPACK |
| [`cdli/whisper-small-Swahili_finetuned_small_CV20`](https://huggingface.co/cdli/whisper-small-Swahili_finetuned_small_CV20) | Whisper-small fine-tuned on Swahili (Common Voice 20 per name) | small | none shown | Exportable path via optimum-executorch (Whisper supported), not tested. WER unverified. |
| [`Mollel/ASR-Swahili-Small`](https://huggingface.co/Mollel/ASR-Swahili-Small) | Whisper-small, Common Voice 17 | small | Apache-2.0 | WER unverified |
| [`smutuvi/finetuning-whisper-tiny-swahili-asr-model_ndizi`](https://huggingface.co/smutuvi/finetuning-whisper-tiny-swahili-asr-model_ndizi) | Whisper-tiny Swahili | 37.8M | Apache-2.0 | smallest Swahili Whisper found, WER unverified |
| [`badrex/w2v-bert-2.0-swahili-asr`](https://huggingface.co/badrex/w2v-bert-2.0-swahili-asr) | w2v-BERT 2.0 CTC | 580.5M | CC-BY-4.0 | Too big for phone, good teacher or server model |
| [`badrex/w2v-bert-2.0-kinyarwanda-asr`](https://huggingface.co/badrex/w2v-bert-2.0-kinyarwanda-asr) | Kinyarwanda, trained on `badrex/kinyarwanda-speech-1000h` | 580M class | CC-BY-4.0 | 14K downloads |
| [`Sunbird/SunflowerASR-51-african-languages`](https://huggingface.co/Sunbird/SunflowerASR-51-african-languages) | Whisper large-v3 based, 51 languages incl. `swa`, `kin`, `lug`, `fra` | 1.54B | Apache-2.0, gated | Server-side only |
| [`Tonykip/nemotron-3.5-swahili-streaming-asr`](https://huggingface.co/Tonykip/nemotron-3.5-swahili-streaming-asr) | NeMo FastConformer RNNT, Kenyan Swahili | ~0.6B | "other" | Not ExecuTorch compatible out of the box |
| [`facebook/mms-1b-all`](https://huggingface.co/facebook/mms-1b-all) | wav2vec2 with adapters for `sw`, `rw` | 964.8M | **CC-BY-NC-4.0** | Non-commercial, big |

ASR data: [`badrex/swahili-speech-400hr`](https://huggingface.co/datasets/badrex/swahili-speech-400hr) (CC-BY-4.0, 10K-100K rows), [`google/fleurs`](https://huggingface.co/datasets/google/fleurs) (CC-BY-4.0, includes `swh`, `lug`, `lin`, `luo`, French), Common Voice (`mozilla-foundation/common_voice_17_0`, now only via Mozilla Data Collective since Oct 2025), [`Sunbird/salt`](https://huggingface.co/datasets/Sunbird/salt) (CC-BY-SA-4.0, gated, includes `sw`, `lug`, `xog`).

### 4b. TTS

| Repo id | Languages | Size | License | Notes |
|---|---|---|---|---|
| [`facebook/mms-tts-swh`](https://huggingface.co/facebook/mms-tts-swh) | Swahili | 36.3M (VITS) | **CC-BY-NC-4.0** | Small and good fit technically. Non-commercial. VITS export to ExecuTorch is unverified. |
| [`facebook/mms-tts-kin`](https://huggingface.co/facebook/mms-tts-kin) | Kinyarwanda | 36.3M | **CC-BY-NC-4.0** | same |
| [`Sunbird/spark-tts-salt`](https://huggingface.co/Sunbird/spark-tts-salt) | Swahili (`swa`), Luganda, Acholi, etc. | Spark-TTS 0.5B (Qwen2 base) | MIT | Larger. Training data is SALT. |
| [`Sunbird/tts-vits-lug`](https://huggingface.co/Sunbird/tts-vits-lug) | Luganda (not target) | | none shown | |
| [`mnigr/swahili_text_to_speech`](https://huggingface.co/mnigr/swahili_text_to_speech) | Swahili, Llama-based, 0 downloads | | Apache-2.0 | Unproven |
| [`executorch-community/Phonon-31M-EN-ExecuTorch-WebGPU`](https://huggingface.co/executorch-community/Phonon-31M-EN-ExecuTorch-WebGPU) | English only, WebGPU | 31M | Apache-2.0 | Shows a small TTS can ship as ExecuTorch |
| Android system TTS (Google, eSpeak-NG) | sw, fr, rw support unverified | | | Fallback with no model download. Verify per device. |

---

## 5. Agricultural knowledge sources

| Source | What | Terms (verified status) | Use for the KB |
|---|---|---|---|
| [`DigiGreen/farmerchat-queries-large`](https://huggingface.co/datasets/DigiGreen/farmerchat-queries-large) | 1,446,866 farmer Q and bot answers, Aug 2024 to Jun 2026. Kenya 858,674, Nigeria 296,671, India 238,030, Ethiopia 38,114. English only. Fields: asset_type, asset_name, query, response, country, state, month, language. | CC-BY-4.0 (verified on card). Names and phones redacted. | Query-intent mining and test questions. Answers are AI-generated, not expert-verified, so do not copy them into the KB as facts. |
| [`CGIAR/Chi-Metrics-2024`](https://huggingface.co/datasets/CGIAR/Chi-Metrics-2024) | 2,300 anonymized Farmer.Chat interaction and evaluation records, Kenya, Oct 2023 to Apr 2024, en and sw | Apache-2.0 | Small Swahili evaluation sample |
| [`KisanVaani/agriculture-qa-english-only`](https://huggingface.co/datasets/KisanVaani/agriculture-qa-english-only) | English agri QA, 10K-100K rows | Apache-2.0 | Generic. India-leaning, quality unverified. Same rows reappear in `talhakk/agriculture-qa` (25,410) and others. |
| `masakhane/uhura-*`, `afrimmlu` | Eval benchmarks, not agri | MIT / mixed | LLM language checks |
| [CABI PlantwisePlus Knowledge Bank](https://plantwiseplusknowledgebank.org) | Factsheets by partners in PlantwisePlus countries (pests, diseases, Kenya, Uganda, Tanzania, Rwanda, DRC factsheets) | **Terms not confirmed.** Search found mixed signals: some CABI Plantwise documents carry CC-BY-NC, one low-reliability mirror cites CC-BY-SA, a 2020 CABI blog asks for open-access factsheets. No site-wide license page found. Ask `plantwise@cabi.org` per the blog. | High value, but do not bundle until written permission or per-factsheet license is confirmed |
| FAO databases ([terms](https://www.fao.org/contact-us/terms/db-terms-of-use/en/)) | Statistical databases | CC-BY-4.0 unless stated. No business promotion, no implied endorsement, credit FAO. This page covers databases only, not publications. FAO publications terms **unverified**. | FAO fall armyworm and IPM guides are likely usable with attribution. Check each document. |
| KALRO (Kenya Agricultural and Livestock Research Organization) | Crop pest and disease leaflets, extension material | **Unverified.** Not checked. Treat as all rights reserved until a license is found. | Ask KALRO for an MoU |
| Lacuna Fund agriculture datasets | Image, yield, language data | Default CC-BY-4.0 unless noted | See section 1 |
| `masakhane/AfriDocMT` | Health and tech docs translation, includes Swahili | not checked | Possible Swahili domain MT data |
| Public-domain sources: government extension PDFs, university extension guides | | per document | Safest seed content, with attribution |

Plan: write the KB as short original chunks (crop, symptom, cause, action, safety), each carrying a `source_id` and license field. Use FAO CC-BY content and permission-confirmed CABI/KALRO content. Keep Farmer.Chat answers for evaluation only.

---

## 6. Similar projects and competitors

Only the entries marked "(checked)" were confirmed by a source in this session. Others are from general background knowledge and are **unverified**.

| Project | What it does | Offline? | Open or closed |
|---|---|---|---|
| PlantVillage Nuru (Penn State, IITA, FAO) (checked) | Cassava leaf diagnosis from phone photo (mosaic, brown streak, red and green mite damage). Embedded in the PlantVillage app. | Yes, works offline per [IITA](https://iita.org/news-item/african-farmers-get-new-help-against-cassava-diseases-nuru-their-artificially-intelligent-assistant/). Framework (TFLite) unconfirmed. | App free. Model license unverified. |
| Plantix (checked in part) | Photo-based crop diagnosis. Engine also sold as a Crop Health API, which FarmerChat integrates (per Plantix business pages). | Offline mode **unverified** | Closed, commercial API |
| Digital Green Farmer.Chat (checked) | Conversational advisory over text, voice and photo. Over 1.2M farmers, India and East Africa. | Online (cloud) per the sources found | Q and A data published CC-BY-4.0. Code license **unverified**. |
| KaraAgro AI (checked) | Ghana-based crop disease datasets and models (maize, cocoa), Lacuna Fund project | Unverified | Datasets open on Dataverse (license unverified) |
| FAO Fall Armyworm Monitoring and Early Warning System (FAMEWS) (checked) | Farmers and workers report infestation and map spread | Unverified | FAO app |
| Farm.ink (checked) | Nairobi start-up, USAID Fall Armyworm Tech Prize winner (US$150,000) | Unverified | Closed |
| Kenya fall armyworm SMS alerts (checked) | Ministry of Agriculture SMS platform piloted in Western Kenya with Safaricom | SMS | Government |
| Apollo Agriculture | Kenyan smallholder inputs, credit and advisory with agent network | Unverified | Closed |
| iCow | Kenyan SMS and voice farm tips for dairy and crops | SMS, works without smartphone (background knowledge) | Closed |
| Safaricom DigiFarm | Kenyan farmer platform (inputs, credit, tips) over USSD/app | USSD (background knowledge) | Closed |
| Tumaini app | I searched for "Tumaini" and found nothing. The app I know under this name is a fall armyworm and maize disease tool, but I could not confirm it. | Unverified | Unverified |

Gap for Shamba AI v2: no verified product combines offline photo diagnosis, a multilingual (Swahili, French, Kinyarwanda) offline advisory chat and voice on one phone with open licenses.

---

## Licenses and risks

Licenses
- Safe to ship (Hub tag, Apache-2.0 or MIT or CC0/CC-BY): timm backbones, Qwen3, Whisper, SmolLM3, Phi-4-mini (MIT), `AI-Lab-Makerere/beans` (MIT), Tanzania banana (CC0), Project-AgML banana and coffee sets (CC-BY-4.0, attribution needed).
- Share-alike: PlantVillage is CC-BY-SA-3.0. Check whether trained weights count as an adaptation. Get legal advice. The CC0 tag on the `BrandonFors` mirror contradicts upstream, so do not rely on it.
- Custom terms: Gemma models (including `CraneAILabs/swahili-gemma-1b`) follow Gemma Terms of Use. Llama 3.2 follows the Llama community license. Both add use restrictions and notice duties. MobileViT and MobileViTv2 carry Apple "other" licenses.
- Non-commercial: `facebook/mms-tts-swh`, `mms-tts-kin`, `mms-1b-all` are CC-BY-NC-4.0. `Saon110/bd-crop...` is CC-BY-NC-SA-4.0. Do not use in a paid or grant-commercial product.
- Missing license: many community fine-tunes (UlizaLlama, cdli Whisper, `Sunbird/translate-nllb-1.3b-salt`, `brainer-fp66/coffee...`). Treat as not usable until the author confirms.
- Unconfirmed: Kaggle cassava license, KaraAgro Dataverse licenses, CABI PlantwisePlus factsheet terms, KALRO terms, FAO publications terms. Resolve these before release.

Technical and quality risks
- Lab-to-field gap: PlantVillage-trained models fail on real field photos. Train and test on field data and keep a held-out East African test set split by farm or source image.
- Duplicates and augmentation: coffee (58,549) and Hemg PlantVillage (70.3K) likely contain augmented copies. Leakage inflates accuracy.
- Class coverage: no open labeled set found for MLN, Kenyan maize fall armyworm, bean pests, potato late blight from East Africa. Plan data collection with partners (KALRO, NaCRRI, Makerere AI Lab).
- Swahili, French, Kinyarwanda LLM quality is unverified for every sub-2B model here. SmolLM3, Llama 3.2 and Phi-4-mini do not list Swahili or Kinyarwanda. Kinyarwanda has no small LLM found. Mitigation: retrieve from the KB, keep answers short and templated, optionally translate with a separate model, and measure with Uhura and our own farmer questions.
- Hallucinated agronomy advice: pesticide dose and safety statements must come from the KB, not from model memory.
- Export risk: Gemma 3n is not on the optimum-executorch list. VITS (MMS-TTS) export is unverified. No ready XNNPACK .pte for Qwen3 or Gemma 3 was found, so budget time to export and quantize (8da4w) and to test RAM on 3-4 GB phones. 1.7B at 8da4w is a size estimate I did not measure.
- Whisper Swahili: stock tiny/base accuracy is unverified. Fine-tuning on `badrex/swahili-speech-400hr` and on Kinyarwanda data is likely needed. Voice must also handle noisy field audio (see `Sunbird/urban-noise-uganda-61k`, CC-BY-4.0, gated, for augmentation).
- Hub data drift: download counts and tags change. Pin dataset and model revisions (commit hash) in the training config.
