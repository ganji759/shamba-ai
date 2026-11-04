import { describe, expect, it } from "vitest";
import { geohash } from "./geohash";
import { STRINGS } from "./i18n";
import { condition, kb, LANGS, t } from "./kb";
import { centerCropBox, rgbaToChw } from "./preprocess";
import { diagnose, statusOf, type ModelManifest } from "./result";

const manifest: ModelManifest = {
  name: "test",
  version: "1",
  backbone: "b",
  classes: ["a", "b", "c", "d"],
  thresholds: { confident: 0.7, uncertain_below: 0.45 },
  input: { shape: [1, 3, 224, 224] },
  files: [],
};

describe("preprocess", () => {
  it("writes planar RGB in [0, 1]", () => {
    // 2x1 image: red pixel, then blue pixel.
    const rgba = new Uint8ClampedArray([255, 0, 0, 255, 0, 0, 255, 255]);
    expect(Array.from(rgbaToChw(rgba, 2, 1))).toEqual([1, 0, 0, 0, 0, 1]);
  });

  it("matches the training crop: resize short side 256, center crop 224", () => {
    const box = centerCropBox(4000, 3000);
    expect(box.side).toBeCloseTo((3000 * 224) / 256);
    expect(box.sx).toBeCloseTo((4000 - box.side) / 2);
    expect(box.sy).toBeCloseTo((3000 - box.side) / 2);
  });
});

describe("result", () => {
  it("maps probability to status with manifest thresholds", () => {
    expect(statusOf(0.9, manifest.thresholds)).toBe("confident");
    expect(statusOf(0.6, manifest.thresholds)).toBe("likely");
    expect(statusOf(0.2, manifest.thresholds)).toBe("uncertain");
  });

  it("ranks the top classes", () => {
    const d = diagnose([0.1, 0.6, 0.25, 0.05], manifest, 3);
    expect(d.top.map((r) => r.classId)).toEqual(["b", "c", "a"]);
    expect(d.status).toBe("likely");
  });
});

describe("knowledge base", () => {
  it("has full advice in every language for every condition", () => {
    for (const [id, c] of Object.entries(kb.conditions)) {
      for (const { id: lang } of LANGS) {
        expect(c.name[lang], `${id} name ${lang}`).toBeTruthy();
        expect(c.actions[lang]?.length, `${id} actions ${lang}`).toBeGreaterThan(0);
      }
      expect(["none", "medium", "high", "critical"]).toContain(c.urgency);
    }
  });

  it("falls back to English", () => {
    expect(t({ en: "x" } as Record<string, string>, "sw")).toBe("x");
    expect(condition("maize_lethal_necrosis")?.urgency).toBe("critical");
  });

  it("has the same UI keys in every language", () => {
    const keys = Object.keys(STRINGS.en).sort();
    expect(Object.keys(STRINGS.sw).sort()).toEqual(keys);
    expect(Object.keys(STRINGS.fr).sort()).toEqual(keys);
  });
});

describe("geohash", () => {
  it("encodes a known point (Nairobi)", () => {
    expect(geohash(-1.2921, 36.8219, 5)).toBe("kzf0t");
  });
});
