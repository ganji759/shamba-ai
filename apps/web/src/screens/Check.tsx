import { Camera, ImagePlus, Loader2, Sun, Focus, Maximize } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import type { EngineState } from "../App";
import type { Engine } from "../lib/engine";
import { geohash } from "../lib/geohash";
import { STRINGS } from "../lib/i18n";
import { cropOf, type Lang } from "../lib/kb";
import { prepareImage } from "../lib/preprocess";
import { diagnose, type Diagnosis, type ModelManifest } from "../lib/result";
import { queueObservation, saveCheck, type Prefs } from "../lib/store";
import { Result } from "./Result";

interface Props {
  lang: Lang;
  manifest: ModelManifest | null;
  engine: EngineState;
  getEngine: () => Engine | null;
  prefs: Prefs;
  onTimed: (ms: number) => void;
}

interface Done {
  diagnosis: Diagnosis;
  photoUrl: string;
  ms: number;
}

function coarseLocation(): Promise<string | undefined> {
  return new Promise((ok) => {
    if (!navigator.geolocation) return ok(undefined);
    navigator.geolocation.getCurrentPosition(
      (p) => ok(geohash(p.coords.latitude, p.coords.longitude, 5)),
      () => ok(undefined),
      { maximumAge: 3_600_000, timeout: 5000, enableHighAccuracy: false },
    );
  });
}

export function Check({ lang, manifest, engine, getEngine, prefs, onTimed }: Props) {
  const s = STRINGS[lang];
  const camRef = useRef<HTMLInputElement>(null);
  const galRef = useRef<HTMLInputElement>(null);
  const [busy, setBusy] = useState<string | null>(null);
  const [done, setDone] = useState<Done | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => () => {
    if (done) URL.revokeObjectURL(done.photoUrl);
  }, [done]);

  async function onFile(file: File | undefined) {
    if (!file || !manifest) return;
    setError(null);
    const photoUrl = URL.createObjectURL(file);
    setBusy(photoUrl);
    try {
      const eng = getEngine();
      if (!eng) throw new Error("engine");
      const [{ input, thumbnail }, info] = await Promise.all([prepareImage(file), eng.ready]);
      const { probs, ms } = await eng.run(input);
      const d = diagnose(probs, manifest);
      onTimed(ms);
      setDone({ diagnosis: d, photoUrl, ms });
      const now = Date.now();
      await saveCheck({
        id: crypto.randomUUID(),
        createdAt: now,
        classId: d.top[0].classId,
        probability: d.top[0].probability,
        status: d.status,
        top: d.top,
        thumbnail,
        modelVersion: manifest.version,
        backend: info.backend,
        ms,
      });
      if (prefs.shareData && prefs.installId && d.status !== "uncertain") {
        await queueObservation({
          install_id: prefs.installId,
          class_id: d.top[0].classId,
          crop: cropOf(d.top[0].classId),
          confidence: Number(d.top[0].probability.toFixed(3)),
          status: d.status,
          model_version: manifest.version,
          app_version: "2.0.0",
          platform: "web",
          captured_at: new Date(now).toISOString(),
          geohash: await coarseLocation(),
        });
      }
    } catch (e) {
      URL.revokeObjectURL(photoUrl);
      setError(e instanceof Error && e.message === "engine" ? s.engineError : s.photoError);
    } finally {
      setBusy(null);
    }
  }

  if (done) {
    return (
      <Result
        lang={lang}
        diagnosis={done.diagnosis}
        photoUrl={done.photoUrl}
        onAgain={() => {
          setDone(null);
        }}
      />
    );
  }

  if (busy) {
    return (
      <section className="checking" aria-live="polite" aria-busy="true">
        <img src={busy} alt="" className="checking-photo" />
        <p className="checking-label">
          <Loader2 aria-hidden className="spin" /> {s.checking}
        </p>
      </section>
    );
  }

  const blocked = !manifest || engine.kind === "error";
  return (
    <section className="check">
      <input
        ref={camRef}
        type="file"
        accept="image/*"
        capture="environment"
        hidden
        onChange={(e) => onFile(e.target.files?.[0])}
      />
      <input ref={galRef} type="file" accept="image/*" hidden onChange={(e) => onFile(e.target.files?.[0])} />

      <button className="capture" disabled={blocked} onClick={() => camRef.current?.click()}>
        <span className="capture-frame" aria-hidden>
          <span />
          <span />
          <span />
          <span />
        </span>
        <Camera aria-hidden className="capture-icon" />
        <span className="capture-label">{s.checkLeaf}</span>
        <span className="capture-sub">{s.takePhoto}</span>
      </button>

      <button className="btn btn-secondary btn-block" disabled={blocked} onClick={() => galRef.current?.click()}>
        <ImagePlus aria-hidden /> {s.choosePhoto}
      </button>

      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      {engine.kind === "error" && !error && (
        <p className="error" role="alert">
          {s.engineError}
        </p>
      )}

      <section className="tips" aria-labelledby="tips-h">
        <h2 id="tips-h">{s.tipsTitle}</h2>
        <ul>
          <li>
            <Maximize aria-hidden /> {s.tip1}
          </li>
          <li>
            <Sun aria-hidden /> {s.tip2}
          </li>
          <li>
            <Focus aria-hidden /> {s.tip3}
          </li>
        </ul>
        <p className="muted">{s.crops}</p>
      </section>
    </section>
  );
}
