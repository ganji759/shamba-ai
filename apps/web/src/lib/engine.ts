import type { Backend, EngineRequest, EngineResponse } from "./engine-protocol";
import type { ModelManifest } from "./result";

const RUNTIME_WASM_SHA256 = "35a9087d0b6cd86c279e7111adfd54a58156bd993928b20f63348f1043652eb4";
const base = import.meta.env.BASE_URL;
const abs = (p: string) => new URL(base + p, location.href).href;

export interface EngineInfo {
  backend: Backend;
  loadMs: number;
}

/** Main-thread client for the inference worker. One engine per page. */
export class Engine {
  private worker: Worker;
  private nextId = 1;
  private pending = new Map<number, { ok: (r: { probs: Float32Array; ms: number }) => void; fail: (e: Error) => void }>();
  readonly ready: Promise<EngineInfo>;

  constructor(manifest: ModelManifest, preferGpu = true) {
    this.worker = new Worker(new URL("../workers/inference.worker.ts", import.meta.url));
    this.ready = new Promise((resolve, reject) => {
      this.worker.onmessage = (e: MessageEvent<EngineResponse>) => {
        const msg = e.data;
        if (msg.type === "ready") resolve({ backend: msg.backend, loadMs: msg.loadMs });
        else if (msg.type === "result") {
          this.pending.get(msg.id)?.ok({ probs: msg.probs, ms: msg.ms });
          this.pending.delete(msg.id);
        } else if (msg.id !== undefined) {
          this.pending.get(msg.id)?.fail(new Error(msg.message));
          this.pending.delete(msg.id);
        } else reject(new Error(msg.message));
      };
      this.worker.onerror = (e) => reject(new Error(e.message || "The inference worker failed to start."));
    });
    const program = (target: Backend) => {
      const f = manifest.files.find((x) => x.target === target);
      // The sha256 in the URL makes each model version a new, immutable cache entry.
      return f ? { url: abs(`models/${f.file}?v=${f.sha256.slice(0, 16)}`), sha256: f.sha256 } : undefined;
    };
    const init: EngineRequest = {
      type: "init",
      runtime: {
        js: abs(`executorch/executorch_wasm_lib.js?v=${RUNTIME_WASM_SHA256.slice(0, 16)}`),
        wasm: abs(`executorch/executorch_wasm_lib.wasm?v=${RUNTIME_WASM_SHA256.slice(0, 16)}`),
        wasmSha256: RUNTIME_WASM_SHA256,
      },
      programs: { webgpu: program("webgpu"), portable: program("portable") },
      inputShape: manifest.input.shape,
      preferGpu,
    };
    this.worker.postMessage(init);
  }

  async run(input: Float32Array): Promise<{ probs: Float32Array; ms: number }> {
    await this.ready;
    const id = this.nextId++;
    return new Promise((ok, fail) => {
      this.pending.set(id, { ok, fail });
      const req: EngineRequest = { type: "run", id, input };
      this.worker.postMessage(req, [input.buffer]);
    });
  }
}

export async function loadManifest(): Promise<ModelManifest> {
  const res = await fetch(abs("models/manifest.json"));
  if (!res.ok) throw new Error("The model list is missing.");
  return res.json();
}

/** Bytes the farmer downloads once for offline use: runtime + the program for this device. */
export function offlinePackBytes(manifest: ModelManifest, backend: Backend = "portable"): number {
  const runtime = 6_562_188 + 165_877;
  return runtime + (manifest.files.find((f) => f.target === backend)?.bytes ?? 0);
}
