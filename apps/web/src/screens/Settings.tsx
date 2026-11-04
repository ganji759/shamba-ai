import type { EngineState } from "../App";
import { STRINGS } from "../lib/i18n";
import { kb, LANGS, type Lang } from "../lib/kb";
import type { ModelManifest } from "../lib/result";
import type { Prefs } from "../lib/store";

interface Props {
  lang: Lang;
  prefs: Prefs;
  manifest: ModelManifest | null;
  engine: EngineState;
  lastMs: number | null;
  onChange: (p: Partial<Prefs>) => void;
}

const API_URL = import.meta.env.VITE_API_URL as string | undefined;

export function Settings({ lang, prefs, manifest, engine, lastMs, onChange }: Props) {
  const s = STRINGS[lang];
  return (
    <section className="settings">
      <h1>{s.settings}</h1>

      <fieldset className="group">
        <legend>{s.language}</legend>
        <div className="langlist">
          {LANGS.map((l) => (
            <button key={l.id} className="langbtn" aria-pressed={lang === l.id} lang={l.id} onClick={() => onChange({ lang: l.id })}>
              {l.label}
            </button>
          ))}
        </div>
      </fieldset>

      <section className="group">
        <h2>{s.privacy}</h2>
        <p>{s.privacyBody}</p>
        {API_URL && (
          <label className="switch">
            <input type="checkbox" checked={!!prefs.shareData} onChange={(e) => onChange({ shareData: e.target.checked })} />
            <span>
              <strong>{s.shareData}</strong>
              <br />
              <span className="muted">{s.shareDataBody}</span>
            </span>
          </label>
        )}
      </section>

      <section className="group">
        <h2>{s.model}</h2>
        <dl className="facts">
          <dt>{s.model}</dt>
          <dd>
            {manifest ? `${manifest.name} ${manifest.version}` : "-"}
            <br />
            <span className="muted">{manifest?.backbone}</span>
          </dd>
          <dt>{s.backend}</dt>
          <dd>
            {engine.kind === "ready"
              ? engine.info.backend === "webgpu"
                ? s.backendWebgpu
                : s.backendPortable
              : "-"}
            <br />
            <span className="muted">PyTorch ExecuTorch · Wasm</span>
          </dd>
          {lastMs !== null && (
            <>
              <dt>{s.lastCheck}</dt>
              <dd>{lastMs.toFixed(0)} ms</dd>
            </>
          )}
        </dl>
      </section>

      <section className="group">
        <h2>{s.about}</h2>
        <p>{s.aboutBody}</p>
        <p className="muted">{s.reviewNote}</p>
        <p>
          <a href="https://github.com/ganji759/shamba-ai">github.com/ganji759/shamba-ai</a>
        </p>
        <h3>{s.sources}</h3>
        <ul className="bullets">
          {kb.sources.map((src) => (
            <li key={src.url}>
              <a href={src.url}>{src.name}</a>
            </li>
          ))}
        </ul>
      </section>
    </section>
  );
}
