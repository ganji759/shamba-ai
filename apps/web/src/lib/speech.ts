import type { Lang } from "./kb";

const BCP47: Record<Lang, string[]> = { en: ["en"], sw: ["sw"], fr: ["fr"] };

/** A voice for the language, if the device has one. Many Android phones have offline voices. */
export function voiceFor(lang: Lang): SpeechSynthesisVoice | undefined {
  if (typeof speechSynthesis === "undefined") return undefined;
  return speechSynthesis.getVoices().find((v) => BCP47[lang].some((p) => v.lang.toLowerCase().startsWith(p)));
}

export function speak(text: string, lang: Lang, onEnd: () => void): boolean {
  const voice = voiceFor(lang);
  if (!voice) return false;
  speechSynthesis.cancel();
  const u = new SpeechSynthesisUtterance(text);
  u.voice = voice;
  u.lang = voice.lang;
  u.rate = 0.95;
  u.onend = onEnd;
  u.onerror = onEnd;
  speechSynthesis.speak(u);
  return true;
}

export function stopSpeaking() {
  if (typeof speechSynthesis !== "undefined") speechSynthesis.cancel();
}
