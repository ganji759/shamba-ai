"""Geohash helpers. Precision 5 is a cell of about 5 km by 5 km."""

from __future__ import annotations

BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"
PRECISION = 5


def is_valid(value: str) -> bool:
    return 1 <= len(value) <= 12 and all(c in BASE32 for c in value)


def truncate(value: str, precision: int = PRECISION) -> str:
    return value.lower()[:precision]


def decode(value: str) -> tuple[float, float]:
    """Return (lat, lon) of the cell centre."""
    lat = [-90.0, 90.0]
    lon = [-180.0, 180.0]
    even = True
    for char in value:
        bits = BASE32.index(char)
        for shift in range(4, -1, -1):
            rng = lon if even else lat
            mid = (rng[0] + rng[1]) / 2
            if (bits >> shift) & 1:
                rng[0] = mid
            else:
                rng[1] = mid
            even = not even
    return (sum(lat) / 2, sum(lon) / 2)


def encode(lat: float, lon: float, precision: int = PRECISION) -> str:
    lat_r = [-90.0, 90.0]
    lon_r = [-180.0, 180.0]
    out: list[str] = []
    even = True
    bit = 0
    ch = 0
    while len(out) < precision:
        rng, val = (lon_r, lon) if even else (lat_r, lat)
        mid = (rng[0] + rng[1]) / 2
        if val >= mid:
            ch = (ch << 1) | 1
            rng[0] = mid
        else:
            ch <<= 1
            rng[1] = mid
        even = not even
        bit += 1
        if bit == 5:
            out.append(BASE32[ch])
            bit = 0
            ch = 0
    return "".join(out)


def label(value: str) -> str:
    lat, lon = decode(value)
    return f"{value} ({lat:.2f}, {lon:.2f})"


__all__ = ["BASE32", "PRECISION", "decode", "encode", "is_valid", "label", "truncate"]
