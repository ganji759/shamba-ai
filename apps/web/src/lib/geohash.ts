const BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";

/** Standard geohash. Precision 5 is a cell of about 4.9 km x 4.9 km: enough for outbreak maps, not a farm. */
export function geohash(lat: number, lon: number, precision = 5): string {
  let latR: [number, number] = [-90, 90];
  let lonR: [number, number] = [-180, 180];
  let hash = "";
  let bit = 0;
  let ch = 0;
  let even = true;
  while (hash.length < precision) {
    const r = even ? lonR : latR;
    const v = even ? lon : lat;
    const mid = (r[0] + r[1]) / 2;
    if (v >= mid) {
      ch = (ch << 1) | 1;
      r[0] = mid;
    } else {
      ch <<= 1;
      r[1] = mid;
    }
    if (even) lonR = r;
    else latR = r;
    even = !even;
    if (++bit === 5) {
      hash += BASE32[ch];
      bit = 0;
      ch = 0;
    }
  }
  return hash;
}
