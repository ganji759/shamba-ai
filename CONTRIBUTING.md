# Contributing

Thank you for helping farmers. Good first contributions:

- **Field photos.** Photos of sick leaves from East Africa, with the crop, the disease (if confirmed) and the country. Open an issue first. We need consent and a clear licence.
- **Advice review.** Agronomists: check an entry in `knowledge/diseases.json` and open a pull request.
- **Translations.** Native speakers: review Swahili or French, or add a new language (Kinyarwanda, Luganda, Lingala, Amharic). Add the language to every entry in `knowledge/diseases.json` and to `apps/web/src/lib/i18n.ts` and `apps/android/app/src/main/res/values-xx/`.
- **Wrong diagnosis reports.** Use the "Wrong diagnosis" issue template.

## Development

| Part | Setup | Check |
|---|---|---|
| `ml/` | `uv venv --python 3.12 && uv pip install -e ".[dev]" --extra-index-url https://download.pytorch.org/whl/cpu --index-strategy unsafe-best-match` | `ruff check . && pytest` |
| `apps/web/` | `npm ci` | `npm run lint && npm run typecheck && npm test && npm run build` |
| `apps/android/` | JDK 21+, Android SDK | `./gradlew assembleDebug testDebugUnitTest` |
| `services/api/` | `uv sync` | `uv run ruff check . && uv run pytest` |

## Rules

- A new class needs three things in the same pull request: training data in `ml/configs/`, advice in `knowledge/diseases.json` for every language, and a passing `ml/tests/test_contract.py`.
- Do not change the model contract (`docs/architecture.md`) without a version bump in `manifest.json`.
- Commit messages: imperative subject, 72 characters max, short sentences.
