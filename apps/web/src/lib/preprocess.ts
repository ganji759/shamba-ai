/**
 * Image -> model input. Same geometry as training (resize short side to 256, center crop 224):
 * take the center square of side short*224/256 and scale it to 224x224 in one step.
 * The model normalizes inside its graph, so the input is RGB in [0, 1], planar CHW.
 */
export const INPUT_SIZE = 224;
export const RESIZE_SHORT_SIDE = 256;

export function centerCropBox(width: number, height: number, size = INPUT_SIZE, short = RESIZE_SHORT_SIDE) {
  const side = (Math.min(width, height) * size) / short;
  return { sx: (width - side) / 2, sy: (height - side) / 2, side };
}

/** RGBA bytes (canvas ImageData) -> Float32 CHW in [0, 1]. Pure function, unit tested. */
export function rgbaToChw(rgba: Uint8ClampedArray | Uint8Array, width: number, height: number): Float32Array {
  const plane = width * height;
  const out = new Float32Array(3 * plane);
  for (let i = 0; i < plane; i++) {
    out[i] = rgba[i * 4] / 255;
    out[plane + i] = rgba[i * 4 + 1] / 255;
    out[2 * plane + i] = rgba[i * 4 + 2] / 255;
  }
  return out;
}

function canvas(w: number, h: number): OffscreenCanvas | HTMLCanvasElement {
  if (typeof OffscreenCanvas !== "undefined") return new OffscreenCanvas(w, h);
  const c = document.createElement("canvas");
  c.width = w;
  c.height = h;
  return c;
}

export interface Prepared {
  input: Float32Array;
  thumbnail: Blob;
}

/** Decode a photo, build the model input and a small JPEG thumbnail for history. */
export async function prepareImage(file: Blob): Promise<Prepared> {
  const bmp = await createImageBitmap(file, { imageOrientation: "from-image" });
  const { sx, sy, side } = centerCropBox(bmp.width, bmp.height);

  const c = canvas(INPUT_SIZE, INPUT_SIZE);
  const ctx = c.getContext("2d", { willReadFrequently: true }) as CanvasRenderingContext2D;
  // High quality smoothing approximates the antialiased resize used in training.
  ctx.imageSmoothingQuality = "high";
  ctx.drawImage(bmp, sx, sy, side, side, 0, 0, INPUT_SIZE, INPUT_SIZE);
  const input = rgbaToChw(ctx.getImageData(0, 0, INPUT_SIZE, INPUT_SIZE).data, INPUT_SIZE, INPUT_SIZE);

  const tw = 320;
  const scale = tw / Math.max(bmp.width, bmp.height);
  const t = canvas(Math.round(bmp.width * scale), Math.round(bmp.height * scale));
  const tctx = t.getContext("2d") as CanvasRenderingContext2D;
  tctx.imageSmoothingQuality = "high";
  tctx.drawImage(bmp, 0, 0, t.width, t.height);
  bmp.close();
  const thumbnail =
    t instanceof HTMLCanvasElement
      ? await new Promise<Blob>((ok) => t.toBlob((b) => ok(b!), "image/jpeg", 0.8))
      : await t.convertToBlob({ type: "image/jpeg", quality: 0.8 });
  return { input, thumbnail };
}
