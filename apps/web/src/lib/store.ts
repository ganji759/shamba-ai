import { openDB, type DBSchema } from "idb";
import type { Status } from "./result";

export interface CheckRecord {
  id: string;
  createdAt: number;
  classId: string;
  probability: number;
  status: Status;
  top: { classId: string; probability: number }[];
  thumbnail: Blob;
  modelVersion: string;
  backend: string;
  ms: number;
}

export interface Observation {
  install_id: string;
  class_id: string;
  crop: string;
  confidence: number;
  status: Status;
  model_version: string;
  app_version: string;
  platform: "web";
  captured_at: string;
  geohash?: string;
}

interface ShambaDB extends DBSchema {
  checks: { key: string; value: CheckRecord; indexes: { byDate: number } };
  outbox: { key: string; value: Observation & { key: string } };
}

const db = openDB<ShambaDB>("shamba", 1, {
  upgrade(d) {
    d.createObjectStore("checks", { keyPath: "id" }).createIndex("byDate", "createdAt");
    d.createObjectStore("outbox", { keyPath: "key" });
  },
});

export async function saveCheck(r: CheckRecord) {
  await (await db).put("checks", r);
}

export async function listChecks(): Promise<CheckRecord[]> {
  return (await (await db).getAllFromIndex("checks", "byDate")).reverse();
}

export async function deleteCheck(id: string) {
  await (await db).delete("checks", id);
}

export async function queueObservation(o: Observation) {
  await (await db).put("outbox", { ...o, key: `${o.captured_at}:${o.class_id}` });
}

/** Send queued results to the Shamba API when online. Keeps them if the send fails. */
export async function flushOutbox(apiUrl: string): Promise<number> {
  const d = await db;
  const items = await d.getAll("outbox");
  if (!items.length || !navigator.onLine) return 0;
  const observations = items.map(({ key: _key, ...o }) => o);
  const res = await fetch(`${apiUrl}/v1/observations`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ observations }),
  });
  if (!res.ok) return 0;
  const tx = d.transaction("outbox", "readwrite");
  await Promise.all(items.map((i) => tx.store.delete(i.key)));
  await tx.done;
  return items.length;
}

const PREF = "shamba.prefs.v1";
export interface Prefs {
  lang?: "en" | "sw" | "fr";
  onboarded?: boolean;
  shareData?: boolean;
  installId?: string;
}

export function loadPrefs(): Prefs {
  try {
    return JSON.parse(localStorage.getItem(PREF) ?? "{}");
  } catch {
    return {};
  }
}

export function savePrefs(p: Prefs) {
  try {
    localStorage.setItem(PREF, JSON.stringify(p));
  } catch {
    /* private mode: prefs live for this session only */
  }
}
