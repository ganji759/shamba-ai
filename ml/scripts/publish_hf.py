"""Publish the model and the web app to the Hugging Face Hub.

Needs a write token: run `hf auth login` once (or set HF_TOKEN).

    python scripts/publish_hf.py --model-repo Kwaba/shamba-crop-health \
        --space Kwaba/shamba-ai --web-dist ../apps/web/dist
"""

from __future__ import annotations

import argparse
import shutil
import tempfile
from pathlib import Path

from huggingface_hub import HfApi

ROOT = Path(__file__).resolve().parents[1]


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--model-repo", default="Kwaba/shamba-crop-health")
    ap.add_argument("--artifacts", default=str(ROOT / "artifacts"))
    ap.add_argument("--card", default=str(ROOT / "MODEL_CARD.md"))
    ap.add_argument("--version", default="v1.0.0")
    ap.add_argument("--space", default="Kwaba/shamba-ai", help="empty to skip the Space")
    ap.add_argument("--web-dist", default=str(ROOT.parent / "apps" / "web" / "dist"))
    ap.add_argument("--space-readme", default=str(ROOT.parent / "infra" / "hf-space" / "README.md"))
    a = ap.parse_args()

    api = HfApi()
    print(f"signed in as {api.whoami()['name']}")

    api.create_repo(a.model_repo, repo_type="model", exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        stage = Path(tmp)
        for f in Path(a.artifacts).iterdir():
            if f.suffix in {".pte", ".json"}:
                shutil.copy(f, stage / f.name)
        shutil.copy(a.card, stage / "README.md")
        api.upload_folder(
            repo_id=a.model_repo,
            folder_path=stage,
            commit_message=f"Shamba crop-health {a.version}",
        )
    api.create_tag(a.model_repo, tag=a.version, exist_ok=True)
    print(f"model: https://huggingface.co/{a.model_repo}")

    if a.space:
        dist = Path(a.web_dist)
        if not (dist / "index.html").exists():
            raise SystemExit(f"{dist} has no build. Run `VITE_BASE=/ npm run build` in apps/web.")
        api.create_repo(a.space, repo_type="space", space_sdk="static", exist_ok=True)
        with tempfile.TemporaryDirectory() as tmp:
            stage = Path(tmp) / "site"
            shutil.copytree(dist, stage)
            shutil.copy(a.space_readme, stage / "README.md")
            api.upload_folder(
                repo_id=a.space,
                repo_type="space",
                folder_path=stage,
                commit_message=f"Shamba AI web {a.version}",
            )
        print(f"space: https://huggingface.co/spaces/{a.space}")


if __name__ == "__main__":
    main()
