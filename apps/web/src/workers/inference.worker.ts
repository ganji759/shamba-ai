/// <reference lib="webworker" />
/**
 * Runs the ExecuTorch Wasm runtime off the main thread.
 * Backend choice: WebGPU-delegated program when the device has WebGPU,
 * otherwise the portable (CPU) program. Same model, same output.
 */
import type { EngineRequest, EngineResponse } from "../lib/engine-protocol";

interface EtTensor {
  data: ArrayLike<number>;
  delete(): void;
}
interface EtModule {
  forward(inputs: EtTensor[]): EtTensor[] | Promise<EtTensor[]>;
  delete(): void;
}
interface ExecuTorch {
  initWebGPU?: () => Promise<void> | void;
  Module: { load(data: Uint8Array): EtModule };
  Tensor: { fromArray(shape: number[], data: ArrayLike<number>): EtTensor };
}
declare function createExecuTorch(options: { wasmBinary: ArrayBuffer }): Promise<ExecuTorch>;

const scope = self as unknown as DedicatedWorkerGlobalScope;
let et: ExecuTorch | null = null;
let model: EtModule | null = null;
let shape: number[] = [1, 3, 224, 224];

async function fetchVerified(url: string, sha256: string | null): Promise<ArrayBuffer> {
  const res = await fetch(url);
  if (!res.ok) throw new Error(`Download failed (${res.status}): ${url}`);
  const buf = await res.arrayBuffer();
  if (sha256) {
    const digest = new Uint8Array(await crypto.subtle.digest("SHA-256", buf));
    const hex = Array.from(digest, (b) => b.toString(16).padStart(2, "0")).join("");
    if (hex !== sha256) throw new Error(`Checksum mismatch: ${url}`);
  }
  return buf;
}

async function hasWebGPU(): Promise<boolean> {
  const gpu = (navigator as Navigator & { gpu?: { requestAdapter(): Promise<unknown> } }).gpu;
  if (!gpu) return false;
  try {
    return (await gpu.requestAdapter()) !== null;
  } catch {
    return false;
  }
}

function post(msg: EngineResponse) {
  scope.postMessage(msg);
}

scope.onmessage = async (event: MessageEvent<EngineRequest>) => {
  const msg = event.data;
  try {
    if (msg.type === "init") {
      const t0 = performance.now();
      scope.importScripts(msg.runtime.js);
      const wasmBinary = await fetchVerified(msg.runtime.wasm, msg.runtime.wasmSha256);
      et = await createExecuTorch({ wasmBinary });
      shape = msg.inputShape;

      const order = msg.preferGpu && (await hasWebGPU()) ? (["webgpu", "portable"] as const) : (["portable"] as const);
      let lastError: unknown = null;
      for (const backend of order) {
        const file = msg.programs[backend];
        if (!file) continue;
        try {
          if (backend === "webgpu") await et.initWebGPU?.();
          const bytes = new Uint8Array(await fetchVerified(file.url, file.sha256));
          model = et.Module.load(bytes);
          // Warm-up run: compiles GPU pipelines or touches CPU caches once.
          const warm = et.Tensor.fromArray(shape, new Float32Array(shape.reduce((a, b) => a * b)));
          const out = await model.forward([warm]);
          warm.delete();
          out.forEach((o) => o.delete());
          post({ type: "ready", backend, loadMs: performance.now() - t0 });
          return;
        } catch (e) {
          lastError = e;
          model?.delete();
          model = null;
        }
      }
      throw lastError ?? new Error("No program can run on this device.");
    }

    if (msg.type === "run") {
      if (!et || !model) throw new Error("Engine is not ready.");
      const input = et.Tensor.fromArray(shape, msg.input);
      const t0 = performance.now();
      const outputs = await model.forward([input]);
      const ms = performance.now() - t0;
      const probs = Float32Array.from(outputs[0].data);
      input.delete();
      outputs.forEach((o) => o.delete());
      post({ type: "result", id: msg.id, probs, ms });
    }
  } catch (e) {
    post({ type: "error", id: msg.type === "run" ? msg.id : undefined, message: e instanceof Error ? e.message : String(e) });
  }
};
