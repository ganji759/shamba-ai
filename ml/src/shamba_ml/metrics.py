"""Classification metrics without extra dependencies."""

from __future__ import annotations

import numpy as np


def confusion(y_true, y_pred, n: int) -> np.ndarray:
    m = np.zeros((n, n), dtype=np.int64)
    for t, p in zip(y_true, y_pred, strict=True):
        m[t, p] += 1
    return m


def report(y_true, y_pred, classes: list[str]) -> dict:
    m = confusion(y_true, y_pred, len(classes))
    tp = np.diag(m).astype(float)
    prec = np.divide(tp, m.sum(0), out=np.zeros_like(tp), where=m.sum(0) > 0)
    rec = np.divide(tp, m.sum(1), out=np.zeros_like(tp), where=m.sum(1) > 0)
    f1 = np.divide(2 * prec * rec, prec + rec, out=np.zeros_like(tp), where=(prec + rec) > 0)
    present = m.sum(1) > 0
    return {
        "summary": {
            "n": int(m.sum()),
            "accuracy": float(tp.sum() / max(1, m.sum())),
            "macro_f1": float(f1[present].mean()) if present.any() else 0.0,
        },
        "per_class": {
            c: {
                "precision": float(prec[i]),
                "recall": float(rec[i]),
                "f1": float(f1[i]),
                "support": int(m[i].sum()),
            }
            for i, c in enumerate(classes)
        },
        "confusion_matrix": m.tolist(),
        "classes": classes,
    }
