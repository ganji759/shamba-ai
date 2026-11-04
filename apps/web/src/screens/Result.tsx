import { ArrowLeft, Share2, Volume2, VolumeX } from "lucide-react";
import { useEffect, useState } from "react";
import { ConfidenceMeter } from "../components/ConfidenceMeter";
import { UrgencyBand } from "../components/UrgencyBand";
import { STRINGS } from "../lib/i18n";
import { condition, cropOf, kb, t, type Lang } from "../lib/kb";
import type { Diagnosis } from "../lib/result";
import { speak, stopSpeaking, voiceFor } from "../lib/speech";

interface Props {
  lang: Lang;
  diagnosis: Diagnosis;
  photoUrl: string;
  onAgain: () => void;
}

export function Result({ lang, diagnosis, photoUrl, onAgain }: Props) {
  const s = STRINGS[lang];
  const best = diagnosis.top[0];
  const c = condition(best.classId);
  const [speaking, setSpeaking] = useState(false);
  const [note, setNote] = useState<string | null>(null);
  const canSpeak = typeof speechSynthesis !== "undefined" && !!voiceFor(lang);

  useEffect(() => () => stopSpeaking(), []);

  const uncertain = diagnosis.status === "uncertain" || !c;
  const name = c ? t(c.name, lang)! : best.classId;
  const crop = t(kb.crops[cropOf(best.classId)], lang);
  const actions = c ? t(c.actions, lang)! : [];
  const prevention = c ? t(c.prevention, lang)! : [];

  const spoken = uncertain
    ? t(kb.ui.retake_tips, lang)!
    : [name, t(c!.summary, lang), s.doNow, ...actions].join(". ");

  async function share() {
    const text = uncertain
      ? t(kb.ui.retake_tips, lang)!
      : `Shamba AI: ${crop} - ${name} (${s.confidence[diagnosis.status]}).\n${s.doNow}:\n${actions
          .map((a, i) => `${i + 1}. ${a}`)
          .join("\n")}`;
    try {
      if (navigator.share) await navigator.share({ title: "Shamba AI", text });
      else {
        await navigator.clipboard.writeText(text);
        setNote(s.shared);
      }
    } catch {
      /* the farmer closed the share sheet */
    }
  }

  return (
    <article className="result">
      <button className="btn btn-quiet back" onClick={onAgain}>
        <ArrowLeft aria-hidden /> {s.checkAnother}
      </button>

      <div className="result-grid">
        <figure className="result-photo">
          <img src={photoUrl} alt="" />
        </figure>

        <div className="result-body">
          {uncertain ? (
            <>
              <h1 className="result-name">{s.confidence.uncertain}</h1>
              <p className="lead">{t(kb.ui.retake_tips, lang)}</p>
            </>
          ) : (
            <>
              <p className="result-crop">{crop}</p>
              <h1 className="result-name">{name}</h1>
              <ConfidenceMeter label={s.confidence[diagnosis.status]} value={best.probability} />
              <UrgencyBand urgency={c!.urgency} label={s.urgency[c!.urgency]} />
              <p className="lead">{t(c!.summary, lang)}</p>

              <section className="advice">
                <h2>{s.doNow}</h2>
                <ol className="numbered">
                  {actions.map((a) => (
                    <li key={a}>{a}</li>
                  ))}
                </ol>
              </section>

              <section className="advice">
                <h2>{s.prevent}</h2>
                <ul className="bullets">
                  {prevention.map((a) => (
                    <li key={a}>{a}</li>
                  ))}
                </ul>
              </section>

              {c!.escalate && (
                <section className="advice escalate">
                  <h2>{s.callOfficer}</h2>
                  <p>{t(c!.escalate, lang)}</p>
                </section>
              )}
              {c!.type !== "healthy" && <p className="safety">{t(kb.ui.safety, lang)}</p>}
            </>
          )}

          <div className="actions">
            {canSpeak && (
              <button
                className="btn btn-secondary"
                onClick={() => {
                  if (speaking) {
                    stopSpeaking();
                    setSpeaking(false);
                  } else setSpeaking(speak(spoken, lang, () => setSpeaking(false)));
                }}
              >
                {speaking ? <VolumeX aria-hidden /> : <Volume2 aria-hidden />}
                {speaking ? s.stopReading : s.readAloud}
              </button>
            )}
            <button className="btn btn-secondary" onClick={share}>
              <Share2 aria-hidden /> {s.share}
            </button>
          </div>
          {note && <p className="muted" role="status">{note}</p>}

          {diagnosis.top.length > 1 && (
            <section className="others">
              <h2>{s.otherPossible}</h2>
              <ul>
                {diagnosis.top.slice(1).map((r) => (
                  <li key={r.classId}>
                    <span>{t(condition(r.classId)?.name, lang) ?? r.classId}</span>
                    <span className="pct">{Math.round(r.probability * 100)}%</span>
                  </li>
                ))}
              </ul>
            </section>
          )}
          <p className="disclaimer">{t(kb.ui.disclaimer, lang)}</p>
        </div>
      </div>
    </article>
  );
}
