import { Download, Trash2 } from "lucide-react";
import { useEffect, useState } from "react";
import { UrgencyBand } from "../components/UrgencyBand";
import { STRINGS } from "../lib/i18n";
import { condition, cropOf, kb, t, type Lang } from "../lib/kb";
import { deleteCheck, listChecks, type CheckRecord } from "../lib/store";

interface Row extends CheckRecord {
  url: string;
}

function toCsv(rows: CheckRecord[]): string {
  const head = "date,crop,class_id,probability,status,model_version,backend,ms";
  const body = rows.map((r) =>
    [new Date(r.createdAt).toISOString(), cropOf(r.classId), r.classId, r.probability.toFixed(3), r.status, r.modelVersion, r.backend, r.ms.toFixed(0)].join(","),
  );
  return [head, ...body].join("\n");
}

export function History({ lang }: { lang: Lang }) {
  const s = STRINGS[lang];
  const [rows, setRows] = useState<Row[] | null>(null);

  useEffect(() => {
    let urls: string[] = [];
    listChecks().then((list) => {
      const withUrls = list.map((r) => ({ ...r, url: URL.createObjectURL(r.thumbnail) }));
      urls = withUrls.map((r) => r.url);
      setRows(withUrls);
    });
    return () => urls.forEach((u) => URL.revokeObjectURL(u));
  }, []);

  if (!rows) return null;
  if (!rows.length) return <p className="empty">{s.noHistory}</p>;

  const fmt = new Intl.DateTimeFormat(lang, { dateStyle: "medium", timeStyle: "short" });
  function exportCsv() {
    const blob = new Blob([toCsv(rows!)], { type: "text/csv" });
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = "shamba-checks.csv";
    a.click();
    URL.revokeObjectURL(a.href);
  }

  return (
    <section className="history">
      <div className="history-head">
        <h1>{s.history}</h1>
        <button className="btn btn-quiet" onClick={exportCsv}>
          <Download aria-hidden /> {s.exportCsv}
        </button>
      </div>
      <ul className="history-list">
        {rows.map((r) => {
          const c = condition(r.classId);
          const uncertain = r.status === "uncertain";
          return (
            <li key={r.id} className="history-item">
              <img src={r.url} alt="" />
              <div>
                <p className="history-crop">
                  {t(kb.crops[cropOf(r.classId)], lang)} · {fmt.format(r.createdAt)}
                </p>
                <p className="history-name">{uncertain ? s.confidence.uncertain : t(c?.name, lang)}</p>
                {c && !uncertain && <UrgencyBand urgency={c.urgency} label={s.urgency[c.urgency]} />}
              </div>
              <button
                className="iconbtn"
                aria-label={s.delete}
                onClick={async () => {
                  await deleteCheck(r.id);
                  URL.revokeObjectURL(r.url);
                  setRows((old) => old!.filter((x) => x.id !== r.id));
                }}
              >
                <Trash2 aria-hidden />
              </button>
            </li>
          );
        })}
      </ul>
    </section>
  );
}
