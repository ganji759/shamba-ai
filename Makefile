# Shamba AI: common tasks. Each target runs in its own folder.
.PHONY: help ml-setup data train export bench web web-dev android api test

PY := ml/.venv/bin

help:
	@echo "ml-setup  Python env for training and export (CPU torch)"
	@echo "data      download East African field photos from Hugging Face"
	@echo "train     fine-tune the crop-health model"
	@echo "export    export ExecuTorch programs to ml/artifacts"
	@echo "bench     measure .pte latency on this machine"
	@echo "web-dev   run the web app"
	@echo "web       build the web app"
	@echo "android   build the Android app"
	@echo "api       run the sync API locally"
	@echo "test      run every test suite"

ml-setup:
	cd ml && uv venv --python 3.12 && uv pip install -e ".[dev]" \
	  --extra-index-url https://download.pytorch.org/whl/cpu --index-strategy unsafe-best-match

data:
	cd ml && .venv/bin/shamba prepare

train:
	cd ml && .venv/bin/shamba train --epochs 10 --out runs/v1

export:
	cd ml && .venv/bin/shamba evaluate --checkpoint runs/v1/best.pt --out runs/v1/metrics.json
	cd ml && .venv/bin/shamba export --checkpoint runs/v1/best.pt --out artifacts \
	  --targets xnnpack-int8,xnnpack,webgpu,portable,vulkan

bench:
	cd ml && .venv/bin/shamba bench --models artifacts

web-dev:
	cd apps/web && npm ci && npm run dev

web:
	cd apps/web && npm ci && npm run build

android:
	cd apps/android && ./gradlew assembleDebug

api:
	cd services/api && uv sync && uv run uvicorn shamba_api.main:app --reload --port 8080

test:
	cd ml && .venv/bin/ruff check . && .venv/bin/pytest -q
	cd apps/web && npm run lint && npm run typecheck && npm test
	cd services/api && uv run pytest -q
	cd apps/android && ./gradlew testDebugUnitTest
