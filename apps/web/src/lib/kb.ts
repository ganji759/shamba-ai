import diseases from "@knowledge/diseases.json";

export type Lang = "en" | "sw" | "fr";
export type Urgency = "none" | "medium" | "high" | "critical";
type Text = Record<Lang, string>;
type TextList = Record<Lang, string[]>;

export interface Condition {
  crop: string;
  type: "fungus" | "bacteria" | "virus" | "pest" | "healthy";
  scientific?: string;
  urgency: Urgency;
  name: Text;
  summary: Text;
  actions: TextList;
  prevention: TextList;
  escalate?: Text;
}

export interface KnowledgeBase {
  version: string;
  review: { status: string; note: string };
  ui: Record<"retake_tips" | "safety" | "disclaimer", Text>;
  crops: Record<string, Text>;
  conditions: Record<string, Condition>;
  sources: { name: string; url: string }[];
}

export const kb = diseases as unknown as KnowledgeBase;
export const LANGS: { id: Lang; label: string }[] = [
  { id: "sw", label: "Kiswahili" },
  { id: "en", label: "English" },
  { id: "fr", label: "Français" },
];

/** Pick the text for a language. Fall back to English when a translation is missing. */
export function t<T>(value: Partial<Record<Lang, T>> | undefined, lang: Lang): T | undefined {
  if (!value) return undefined;
  return value[lang] ?? value.en;
}

export function condition(classId: string): Condition | undefined {
  return kb.conditions[classId];
}

export function cropOf(classId: string): string {
  return classId.split("_", 1)[0];
}
