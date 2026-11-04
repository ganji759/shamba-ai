import { BookOpen, History as HistoryIcon, Leaf, Settings as SettingsIcon, Wifi, WifiOff } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { Check } from "./screens/Check";
import { History } from "./screens/History";
import { Library } from "./screens/Library";
import { Onboarding } from "./screens/Onboarding";
import { Settings } from "./screens/Settings";
import { Engine, loadManifest, type EngineInfo } from "./lib/engine";
import { STRINGS } from "./lib/i18n";
import type { Lang } from "./lib/kb";
import type { ModelManifest } from "./lib/result";
import { flushOutbox, loadPrefs, savePrefs, type Prefs } from "./lib/store";

type Tab = "check" | "history" | "library" | "settings";
export type EngineState =
  | { kind: "idle" }
  | { kind: "loading" }
  | { kind: "ready"; info: EngineInfo }
  | { kind: "error"; message: string };

const API_URL = import.meta.env.VITE_API_URL as string | undefined;

let engineSingleton: Engine | null = null;

export function App() {
  const [prefs, setPrefs] = useState<Prefs>(() => {
    const p = loadPrefs();
    return p.installId ? p : { ...p, installId: crypto.randomUUID() };
  });
  const [tab, setTab] = useState<Tab>("check");
  const [manifest, setManifest] = useState<ModelManifest | null>(null);
  const [engine, setEngine] = useState<EngineState>({ kind: "idle" });
  const [online, setOnline] = useState(navigator.onLine);
  const [lastMs, setLastMs] = useState<number | null>(null);

  const lang: Lang = prefs.lang ?? "sw";
  const s = STRINGS[lang];

  const update = useCallback((p: Partial<Prefs>) => {
    setPrefs((old) => {
      const next = { ...old, ...p };
      savePrefs(next);
      return next;
    });
  }, []);

  const startEngine = useCallback((m: ModelManifest | null) => {
    if (!m || engineSingleton) return engineSingleton;
    setEngine({ kind: "loading" });
    engineSingleton = new Engine(m);
    engineSingleton.ready.then(
      (info) => setEngine({ kind: "ready", info }),
      (e: Error) => {
        engineSingleton = null;
        setEngine({ kind: "error", message: e.message });
      },
    );
    return engineSingleton;
  }, []);

  useEffect(() => {
    document.documentElement.lang = lang;
  }, [lang]);

  useEffect(() => {
    loadManifest().then(
      (m) => {
        setManifest(m);
        // A returning user already has the offline pack in cache: start at once.
        if (loadPrefs().onboarded) startEngine(m);
      },
      (e) => setEngine({ kind: "error", message: String(e) }),
    );
    const on = () => setOnline(true);
    const off = () => setOnline(false);
    addEventListener("online", on);
    addEventListener("offline", off);
    return () => {
      removeEventListener("online", on);
      removeEventListener("offline", off);
    };
  }, [startEngine]);

  useEffect(() => {
    if (online && prefs.shareData && API_URL) flushOutbox(API_URL).catch(() => undefined);
  }, [online, prefs.shareData]);


  const nav = useMemo(
    () => [
      { id: "check" as const, label: s.check, icon: Leaf },
      { id: "history" as const, label: s.history, icon: HistoryIcon },
      { id: "library" as const, label: s.library, icon: BookOpen },
    ],
    [s],
  );

  if (!prefs.onboarded) {
    return (
      <Onboarding
        lang={lang}
        manifest={manifest}
        engine={engine}
        onLang={(l) => update({ lang: l })}
        onDownload={() => startEngine(manifest)}
        onDone={() => update({ onboarded: true })}
      />
    );
  }

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">
          <Leaf aria-hidden className="brand-mark" />
          <span>Shamba AI</span>
        </div>
        <span className={`netchip ${online ? "is-online" : "is-offline"}`}>
          {online ? <Wifi aria-hidden size={16} /> : <WifiOff aria-hidden size={16} />}
          {engine.kind === "ready" ? s.offlineReady : online ? s.online : s.offline}
        </span>
        <button
          className="iconbtn"
          aria-label={s.settings}
          aria-pressed={tab === "settings"}
          onClick={() => setTab(tab === "settings" ? "check" : "settings")}
        >
          <SettingsIcon aria-hidden />
        </button>
      </header>

      <main className="main" id="main">
        {tab === "check" && (
          <Check
            lang={lang}
            manifest={manifest}
            engine={engine}
            getEngine={() => startEngine(manifest)}
            prefs={prefs}
            onTimed={setLastMs}
          />
        )}
        {tab === "history" && <History lang={lang} />}
        {tab === "library" && <Library lang={lang} />}
        {tab === "settings" && (
          <Settings lang={lang} prefs={prefs} manifest={manifest} engine={engine} lastMs={lastMs} onChange={update} />
        )}
      </main>

      <nav className="tabbar" aria-label="Main">
        {nav.map(({ id, label, icon: Icon }) => (
          <button key={id} className="tab" aria-current={tab === id ? "page" : undefined} onClick={() => setTab(id)}>
            <Icon aria-hidden />
            <span>{label}</span>
          </button>
        ))}
      </nav>
    </div>
  );
}
