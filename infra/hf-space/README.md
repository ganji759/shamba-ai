---
title: Shamba AI
emoji: 🌱
colorFrom: green
colorTo: green
sdk: static
app_file: index.html
pinned: false
license: apache-2.0
short_description: Offline-first crop health assistant for East Africa
---

# Shamba AI web app

This folder holds the front matter for the Hugging Face static Space.

The CI workflow `.github/workflows/release-model.yml` copies the web build (`apps/web/dist`) into the Space. It keeps this README as the Space README. Do not put the built files in this folder by hand.

The app runs the diagnosis in the browser. The Space only serves the files.
