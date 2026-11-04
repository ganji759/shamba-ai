"""Server-rendered HTML dashboard. No external assets."""

from __future__ import annotations

from html import escape
from urllib.parse import quote

from .knowledge import Knowledge
from .schemas import OutbreakReport

CSS = """
:root{--bg:#f6f7f4;--fg:#1d2a22;--card:#fff;--line:#d9dfd6;--accent:#2f7d4f;--warn:#b3261e}
@media (prefers-color-scheme:dark){:root{--bg:#141a16;--fg:#e6ebe6;--card:#1d2620;
--line:#2d3a31;--accent:#6fcf97;--warn:#ff8a80}}
*{box-sizing:border-box}
body{margin:0;background:var(--bg);color:var(--fg);font:16px/1.5 system-ui,sans-serif}
main{max-width:960px;margin:0 auto;padding:16px}
h1{font-size:1.4rem;margin:.5rem 0}h2{font-size:1.1rem;margin:1.5rem 0 .5rem}
.tiles{display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:12px}
.tile{background:var(--card);border:1px solid var(--line);border-radius:8px;padding:12px}
.tile b{display:block;font-size:1.6rem}
.alert{background:var(--card);border:1px solid var(--warn);border-left-width:6px;
border-radius:8px;padding:8px 12px;margin:8px 0}
.wrap{overflow-x:auto}
table{width:100%;border-collapse:collapse;background:var(--card)}
th,td{padding:6px 10px;border-bottom:1px solid var(--line);text-align:left;white-space:nowrap}
.up{color:var(--warn);font-weight:600}.muted{opacity:.7}
"""


def _tile(label: str, value: object) -> str:
    return f'<div class="tile"><b>{escape(str(value))}</b>{escape(label)}</div>'


def render(report: OutbreakReport, knowledge: Knowledge, key: str | None = None) -> str:
    rows = report.rows
    alerts = [r for r in rows if r.class_id in knowledge.critical and r.trend in ("rising", "new")]
    tiles = "".join(
        [
            _tile("reports in period", sum(r.count for r in rows)),
            _tile("areas (about 5 km)", len({r.geohash for r in rows})),
            _tile("conditions seen", len({r.class_id for r in rows})),
            _tile("critical alerts", len(alerts)),
        ]
    )
    if alerts:
        alert_html = "".join(
            f'<div class="alert"><b>{escape(knowledge.names.get(a.class_id, a.class_id))}</b>'
            f" in area {escape(a.geohash)}: {a.count} reports "
            f"({escape(a.trend)}, previous period {a.previous_count}).</div>"
            for a in alerts
        )
    else:
        alert_html = '<p class="muted">No rising critical conditions.</p>'
    body = (
        "".join(
            "<tr>"
            f"<td>{escape(r.geohash)}</td>"
            f"<td>{r.lat:.2f}, {r.lon:.2f}</td>"
            f"<td>{escape(knowledge.names.get(r.class_id, r.class_id))}</td>"
            f"<td>{escape(r.crop)}</td><td>{r.count}</td><td>{r.installs}</td>"
            f"<td>{r.previous_count}</td>"
            f'<td class="{"up" if r.trend in ("rising", "new") else ""}">{escape(r.trend)}</td>'
            "</tr>"
            for r in rows
        )
        or '<tr><td colspan="8" class="muted">No area has enough data yet.</td></tr>'
    )
    crop = escape(report.crop) if report.crop else "all crops"
    csv_href = "/v1/outbreaks.csv?days=" + str(report.days)
    if report.crop:
        csv_href += "&amp;crop=" + escape(report.crop)
    if key:
        csv_href += "&amp;key=" + escape(quote(key))
    return f"""<!doctype html>
<html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Shamba AI outbreak dashboard</title><style>{CSS}</style></head>
<body><main>
<h1>Shamba AI outbreak dashboard</h1>
<p class="muted">Last {report.days} days, {crop}. Areas need at least
{report.k_anonymity} distinct installs to appear.</p>
<div class="tiles">{tiles}</div>
<h2>Top alerts</h2>{alert_html}
<h2>By area</h2>
<div class="wrap"><table>
<tr><th>Area</th><th>Centre</th><th>Condition</th><th>Crop</th><th>Reports</th>
<th>Installs</th><th>Previous</th><th>Trend</th></tr>{body}</table></div>
<p><a href="{csv_href}">Download CSV</a>
(send the key in the X-API-Key header for scripts)</p>
</main></body></html>"""
