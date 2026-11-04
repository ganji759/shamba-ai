// Put the offline pack into public/ before dev or build:
//   public/executorch/  ExecuTorch Wasm + WebGPU runtime, pinned by sha256
//   public/models/      browser .pte programs and manifest.json from ml/artifacts
// The repo keeps one source of truth: ml/artifacts for models, Hugging Face for the runtime.
import { createHash } from "node:crypto";
import { copyFileSync, existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const root = dirname(dirname(fileURLToPath(import.meta.url)));
const repo = join(root, "..", "..");

const RUNTIME_REPO = "executorch-community/ExecuTorch-WebGPU-Runtime";
const RUNTIME_FILES = {
  "executorch_wasm_lib.js": "8a631b1537927f8f2ef184d9f1b343061f47df0a32ddb8f5190d74e80d40618e",
  "executorch_wasm_lib.wasm": "35a9087d0b6cd86c279e7111adfd54a58156bd993928b20f63348f1043652eb4",
  LICENSE: null,
  "THIRD_PARTY_NOTICES.txt": null,
};
const BROWSER_TARGETS = ["webgpu", "portable"];

const sha256 = (buf) => createHash("sha256").update(buf).digest("hex");

async function syncRuntime() {
  const dir = join(root, "public", "executorch");
  mkdirSync(dir, { recursive: true });
  for (const [file, expected] of Object.entries(RUNTIME_FILES)) {
    const dest = join(dir, file);
    if (existsSync(dest) && (!expected || sha256(readFileSync(dest)) === expected)) continue;
    const url = `https://huggingface.co/${RUNTIME_REPO}/resolve/main/${file}`;
    process.stdout.write(`download ${url}\n`);
    const res = await fetch(url);
    if (!res.ok) throw new Error(`${url}: HTTP ${res.status}`);
    const buf = Buffer.from(await res.arrayBuffer());
    if (expected && sha256(buf) !== expected) throw new Error(`${file}: sha256 mismatch`);
    writeFileSync(dest, buf);
  }
}

// CI and fresh clones have no local model: fetch the published release instead.
const MODEL_RELEASE =
  process.env.SHAMBA_MODEL_URL ?? "https://github.com/ganji759/shamba-ai/releases/download/model-v1.0.0";

async function download(url) {
  const res = await fetch(url, { redirect: "follow" });
  if (!res.ok) throw new Error(`${url}: HTTP ${res.status}`);
  return Buffer.from(await res.arrayBuffer());
}

async function syncModels() {
  const candidates = [join(repo, "ml", "artifacts"), join(repo, "ml", "runs", "smoke", "artifacts")];
  const src = candidates.find((d) => existsSync(join(d, "manifest.json")));
  const dir = join(root, "public", "models");
  mkdirSync(dir, { recursive: true });
  const manifest = src
    ? JSON.parse(readFileSync(join(src, "manifest.json"), "utf8"))
    : JSON.parse((await download(`${MODEL_RELEASE}/manifest.json`)).toString("utf8"));
  const files = manifest.files.filter((f) => BROWSER_TARGETS.includes(f.target));
  for (const f of files) {
    const dest = join(dir, f.file);
    if (src) copyFileSync(join(src, f.file), dest);
    else if (!existsSync(dest) || sha256(readFileSync(dest)) !== f.sha256) {
      const buf = await download(`${MODEL_RELEASE}/${f.file}`);
      if (sha256(buf) !== f.sha256) throw new Error(`${f.file}: sha256 mismatch`);
      writeFileSync(dest, buf);
    }
  }
  // The browser manifest lists only the programs the browser can run.
  writeFileSync(join(dir, "manifest.json"), JSON.stringify({ ...manifest, files }, null, 2));
  process.stdout.write(`models from ${src ?? MODEL_RELEASE}: ${files.map((f) => f.file).join(", ")}\n`);
}

await syncRuntime();
await syncModels();
