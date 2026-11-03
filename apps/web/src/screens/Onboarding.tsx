import { Download, Leaf, Loader2 } from "lucide-react";
import { useState } from "react";
import type { EngineState } from "../App";
import { offlinePackBytes } from "../lib/engine";
import { STRINGS } from "../lib/i18n";
import { LANGS, type Lang } from "../lib/kb";
import type { ModelManifest } from "../lib/result";

interface Props {
  lang: Lang;
  manifest: ModelManifest | null;
  engine: EngineState;
  onLang: (l: Lang) => void;
  onDownload: () => void;
  onDone: () => void;
}

export function Onboarding({ lang, manifest, engine, onLang, onDownload, onDone }: Props) {
  const [step, setStep] = useState<"lang" | "how">("lang");
  const s = STRINGS[lang];
  const mb = manifest ? (offlinePackBytes(manifest) / 1e6).toFixed(0) : "17";

  if (step === "lang") {
    return (
      <div className="onboard">
        <div className="onboard-hero">
          <Leaf aria-hidden className="onboard-leaf" />
          <h1>Shamba AI</h1>
          <p>{s.tagline}</p>
        </div>
        <h2 className="onboard-q">{s.chooseLanguage}</h2>
        <div className="langlist" role="radiogroup" aria-label={s.chooseLanguage}>
          {LANGS.map((l) => (
            <button
              key={l.id}
              role="radio"
              aria-checked={lang === l.id}
              className="langbtn"
              lang={l.id}
              onClick={() => onLang(l.id)}
            >
              {l.label}
            </button>
          ))}
        </div>
        <button className="btn btn-primary btn-block" onClick={() => setStep("how")}>
          {s.continue}
        </button>
      </div>
    );
  }

  const busy = engine.kind === "loading";
  return (
    <div className="onboard">
      <h1 className="onboard-title">{s.howTitle}</h1>
      <ol className="steps">
        <li>{s.how1}</li>
        <li>{s.how2}</li>
        <li>{s.how3}</li>
      </ol>
      <section className="pack" aria-live="polite">
        <h2>{s.offlinePack}</h2>
        <p>{s.offlinePackBody.replace("{mb}", mb)}</p>
        {engine.kind === "error" && <p className="error">{s.engineError}</p>}
        {engine.kind === "ready" ? (
          <button className="btn btn-primary btn-block" onClick={onDone}>
            {s.continue}
          </button>
        ) : (
          <button className="btn btn-primary btn-block" disabled={busy || !manifest} onClick={onDownload}>
            {busy ? <Loader2 aria-hidden className="spin" /> : <Download aria-hidden />}
            {busy ? s.downloading : s.download}
          </button>
        )}
        {engine.kind !== "ready" && (
          <button className="btn btn-quiet btn-block" onClick={onDone}>
            {s.later}
          </button>
        )}
      </section>
    </div>
  );
}
