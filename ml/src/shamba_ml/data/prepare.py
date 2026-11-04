"""Download field photos from the Hugging Face Hub and build one local image set.

Each source in the YAML config maps its own labels to Shamba class ids. The script
reads only the parquet row groups it needs, so it stops early when every class
reaches its cap. Images are resized (short side) and saved as JPEG, which keeps
the local set small enough for CPU training.

Output layout:
    <out>/images/<class_id>/<source>_<n>.jpg
    <out>/manifest.csv   columns: path,label,source,split
"""

from __future__ import annotations

import csv
import io
import json
import random
from collections import Counter, defaultdict
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass
from pathlib import Path

import pyarrow.parquet as pq
from huggingface_hub import HfFileSystem
from PIL import Image, ImageOps

from shamba_ml.config import Config, load_config


@dataclass
class Sample:
    path: str
    label: str
    source: str
    split: str | None  # None = assign later


def _save(img_bytes: bytes, dest: Path, short_side: int) -> bool:
    try:
        img = Image.open(io.BytesIO(img_bytes))
        # draft() makes the JPEG decoder skip full-size decode: fast and low memory.
        img.draft("RGB", (short_side * 2, short_side * 2))
        img = ImageOps.exif_transpose(img).convert("RGB")
        w, h = img.size
        scale = short_side / min(w, h)
        if scale < 1:
            img = img.resize((round(w * scale), round(h * scale)), Image.BILINEAR)
        dest.parent.mkdir(parents=True, exist_ok=True)
        img.save(dest, "JPEG", quality=90)
        return True
    except Exception as e:  # noqa: BLE001 - a bad image must not stop the run
        print(f"  skip {dest.name}: {e}")
        return False


def _class_names(pf: pq.ParquetFile, column: str) -> list[str] | None:
    meta = pf.schema_arrow.metadata or {}
    raw = meta.get(b"huggingface")
    if not raw:
        return None
    feats = json.loads(raw)["info"]["features"]
    return feats.get(column, {}).get("names")


def _split_from_path(path: str) -> str | None:
    for name, split in (("/train/", "train"), ("/validation/", "val"), ("/test/", "test")):
        if name in path:
            return split
    return None


def prepare_parquet(src: dict, cfg: Config, out: Path, fs: HfFileSystem) -> list[Sample]:
    rev = src.get("revision")
    base = f"datasets/{src['repo']}" + (f"@{rev.replace('/', '%2F')}" if rev else "")
    files = sorted(fs.glob(f"{base}/{src['pattern']}"))
    cap = src.get("per_class_cap")
    counts: Counter[str] = Counter()
    samples: list[Sample] = []
    rng = random.Random(cfg.seed)

    # Visit row groups in random order so a capped class gets a spread of files.
    groups = []
    for f in files:
        with fs.open(f) as fh:
            groups += [(f, i) for i in range(pq.ParquetFile(fh).num_row_groups)]
    rng.shuffle(groups)
    print(f"[{src['id']}] {len(files)} files, {len(groups)} row groups")

    names = None
    for f, gi in groups:
        if cap and all(counts[c] >= cap for c in src["label_map"].values()):
            break
        with fs.open(f) as fh:
            pf = pq.ParquetFile(fh)
            names = names or _class_names(pf, src["label_column"])
            table = pf.read_row_group(gi, columns=["image", src["label_column"]])
        split = _split_from_path(f) if src.get("keep_splits") else None
        for row in table.to_pylist():
            raw_label = row[src["label_column"]]
            if names and isinstance(raw_label, int):
                raw_label = names[raw_label]
            label = src["label_map"].get(str(raw_label))
            if label is None or (cap and counts[label] >= cap):
                continue
            dest = out / "images" / label / f"{src['id']}_{len(samples):06d}.jpg"
            if _save(row["image"]["bytes"], dest, cfg.resize_short_side):
                counts[label] += 1
                samples.append(Sample(str(dest.relative_to(out)), label, src["id"], split))
        print(f"[{src['id']}] {dict(counts)}")
    return samples


def prepare_csv_images(src: dict, cfg: Config, out: Path, fs: HfFileSystem) -> list[Sample]:
    base = f"datasets/{src['repo']}"
    with fs.open(f"{base}/{src['csv']}", "r") as fh:
        rows = list(csv.DictReader(fh))
    rng = random.Random(cfg.seed)
    rng.shuffle(rows)
    cap = src.get("per_class_cap")
    picked: list[tuple[str, str]] = []
    counts: Counter[str] = Counter()
    for r in rows:
        label = src["label_map"].get(str(r[src["label_column"]]))
        if label and (not cap or counts[label] < cap):
            counts[label] += 1
            picked.append((r[src["id_column"]], label))
    print(f"[{src['id']}] downloading {len(picked)} images: {dict(counts)}")
    # Mirrors can shard images into sub-folders. Index every file by its name.
    index = {Path(p).name: p for p in fs.find(f"{base}/{src['image_dir']}")}

    def fetch(i_item):
        i, (image_id, label) = i_item
        dest = out / "images" / label / f"{src['id']}_{i:06d}.jpg"
        if dest.exists():
            return Sample(str(dest.relative_to(out)), label, src["id"], None)
        if image_id not in index:
            return None
        data = fs.cat_file(index[image_id])
        if _save(data, dest, cfg.resize_short_side):
            return Sample(str(dest.relative_to(out)), label, src["id"], None)
        return None

    with ThreadPoolExecutor(max_workers=16) as pool:
        results = list(pool.map(fetch, enumerate(picked)))
    return [s for s in results if s]


def assign_splits(samples: list[Sample], cfg: Config) -> None:
    """Stratified split for samples that do not bring their own split."""
    rng = random.Random(cfg.seed)
    by_label: dict[str, list[Sample]] = defaultdict(list)
    for s in samples:
        if s.split is None:
            by_label[s.label].append(s)
    for group in by_label.values():
        rng.shuffle(group)
        n = len(group)
        n_val, n_test = round(n * cfg.split["val"]), round(n * cfg.split["test"])
        for i, s in enumerate(group):
            s.split = "test" if i < n_test else "val" if i < n_test + n_val else "train"


def run(config_path: str, out_dir: str, only: list[str] | None = None) -> Path:
    cfg = load_config(config_path)
    out = Path(out_dir)
    out.mkdir(parents=True, exist_ok=True)
    fs = HfFileSystem()
    samples: list[Sample] = []
    for src in cfg.sources:
        if only and src["id"] not in only:
            continue
        if src["kind"] == "hf_parquet":
            part = prepare_parquet(src, cfg, out, fs)
        elif src["kind"] == "hf_csv_images":
            part = prepare_csv_images(src, cfg, out, fs)
        else:
            raise ValueError(f"unknown source kind {src['kind']}")
        part_manifest = out / f"manifest.{src['id']}.csv"
        _write_manifest(part, part_manifest)
        samples += part

    # Merge every per-source manifest on disk, so sources can run in separate jobs.
    samples = []
    for f in sorted(out.glob("manifest.*.csv")):
        with f.open() as fh:
            samples += [
                Sample(r["path"], r["label"], r["source"], r["split"] or None)
                for r in csv.DictReader(fh)
            ]
    assign_splits(samples, cfg)
    _write_manifest(samples, out / "manifest.csv")
    stats = Counter((s.label, s.split) for s in samples)
    print(json.dumps({f"{k[0]}/{k[1]}": v for k, v in sorted(stats.items())}, indent=1))
    return out / "manifest.csv"


def _write_manifest(samples: list[Sample], path: Path) -> None:
    with path.open("w", newline="") as fh:
        w = csv.writer(fh)
        w.writerow(["path", "label", "source", "split"])
        for s in samples:
            w.writerow([s.path, s.label, s.source, s.split or ""])
