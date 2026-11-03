export type Backend = "webgpu" | "portable";

export interface ProgramFile {
  url: string;
  sha256: string;
}

export type EngineRequest =
  | {
      type: "init";
      runtime: { js: string; wasm: string; wasmSha256: string };
      programs: Partial<Record<Backend, ProgramFile>>;
      inputShape: number[];
      preferGpu: boolean;
    }
  | { type: "run"; id: number; input: Float32Array };

export type EngineResponse =
  | { type: "ready"; backend: Backend; loadMs: number }
  | { type: "result"; id: number; probs: Float32Array; ms: number }
  | { type: "error"; id?: number; message: string };
