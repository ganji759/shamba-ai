from shamba_api import geohash as gh


def test_roundtrip():
    code = gh.encode(-1.2921, 36.8219, 7)
    lat, lon = gh.decode(code)
    assert abs(lat + 1.2921) < 0.001 and abs(lon - 36.8219) < 0.001
    assert gh.truncate(code) == code[:5]


def test_valid():
    assert gh.is_valid("kzf0w")
    assert not gh.is_valid("kzf0a")
    assert not gh.is_valid("")
