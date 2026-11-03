export type Status = "confident" | "likely" | "uncertain";

export interface ModelManifest {
  name: string;
  version: string;
  backbone: string;
  classes: string[];
  thresholds: { confident: number; uncertain_below: number };
  input: { shape: number[] };
  files: { file: string; target: string; bytes: number; sha256: string }[];
  eval?: { val?: { accuracy: number; macro_f1: number } };
}

export interface Ranked {
  classId: string;
  probability: number;
}

export interface Diagnosis {
  status: Status;
  top: Ranked[];
}

export function statusOf(p: number, th: ModelManifest["thresholds"]): Status {
  if (p >= th.confident) return "confident";
  if (p < th.uncertain_below) return "uncertain";
  return "likely";
}

export function diagnose(probs: ArrayLike<number>, manifest: ModelManifest, k = 3): Diagnosis {
  const top = Array.from(probs, (p, i) => ({ classId: manifest.classes[i], probability: p }))
    .sort((a, b) => b.probability - a.probability)
    .slice(0, k);
  return { status: statusOf(top[0].probability, manifest.thresholds), top };
}
