# Security

## Report a problem

Open a private advisory on GitHub (Security tab, "Report a vulnerability"). Do not open a public issue for a security problem.

## Design

- Photos never leave the device. The apps have no upload path for photos.
- Cloud sync is off by default. When a user turns it on, the app sends only: class id, confidence, status, model version, app version, platform, time, a random install id and a 5-character geohash (about 5 km).
- The outbreak API returns only map cells with at least 5 distinct installs.
- The apps verify the sha256 of each model and runtime file before they load it.
- No API key is in client code. v2 has no client-side cloud AI calls.

## Note on v1

v1 (tag `v1-legacy`) called the Gemini API from the browser. Its Vite config put `GEMINI_API_KEY` into the client bundle at build time. If you deployed v1 with a real key, rotate that key in Google AI Studio and remove the old Cloud Run service. v2 removes this code path.
