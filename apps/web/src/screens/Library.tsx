import { useState } from "react";
import { UrgencyBand } from "../components/UrgencyBand";
import { STRINGS } from "../lib/i18n";
import { kb, t, type Lang } from "../lib/kb";

export function Library({ lang }: { lang: Lang }) {
  const s = STRINGS[lang];
  const crops = Object.keys(kb.crops);
  const [crop, setCrop] = useState(crops[0]);
  const entries = Object.entries(kb.conditions).filter(([, c]) => c.crop === crop && c.type !== "healthy");

  return (
    <section className="library">
      <h1>{s.library}</h1>
      <div className="croptabs" role="tablist">
        {crops.map((id) => (
          <button key={id} role="tab" aria-selected={crop === id} className="croptab" onClick={() => setCrop(id)}>
            {t(kb.crops[id], lang)}
          </button>
        ))}
      </div>
      <div className="library-list" role="tabpanel">
        {entries.map(([id, c]) => (
          <details key={id} className="disease">
            <summary>
              <span className="disease-name">{t(c.name, lang)}</span>
              <UrgencyBand urgency={c.urgency} label={s.urgency[c.urgency]} />
            </summary>
            <p className="lead">{t(c.summary, lang)}</p>
            {c.scientific && <p className="muted sci">{c.scientific}</p>}
            <h2>{s.doNow}</h2>
            <ol className="numbered">
              {t(c.actions, lang)!.map((a) => (
                <li key={a}>{a}</li>
              ))}
            </ol>
            <h2>{s.prevent}</h2>
            <ul className="bullets">
              {t(c.prevention, lang)!.map((a) => (
                <li key={a}>{a}</li>
              ))}
            </ul>
          </details>
        ))}
      </div>
    </section>
  );
}
