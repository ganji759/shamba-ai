import { fileURLToPath } from "node:url";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";
import { VitePWA } from "vite-plugin-pwa";

const knowledge = fileURLToPath(new URL("../../knowledge", import.meta.url));

// The app shell is precached. The model and the ExecuTorch runtime (about 17 MB)
// are cached on first use, after the farmer agrees to the download.
export default defineConfig({
  base: process.env.VITE_BASE ?? "/",
  resolve: { alias: { "@knowledge": knowledge } },
  server: { fs: { allow: [".", knowledge] } },
  // Classic worker: the Emscripten runtime loads with importScripts().
  worker: { format: "iife" },
  plugins: [
    react(),
    VitePWA({
      registerType: "autoUpdate",
      includeAssets: ["icons/*.png", "icons/*.svg"],
      manifest: {
        name: "Shamba AI",
        short_name: "Shamba",
        description: "Check crop leaves for disease. Works offline. Photos stay on your device.",
        theme_color: "#1F6B3A",
        background_color: "#F1F5EF",
        display: "standalone",
        orientation: "portrait",
        start_url: ".",
        lang: "en",
        categories: ["agriculture", "productivity", "education"],
        icons: [
          { src: "icons/icon-192.png", sizes: "192x192", type: "image/png" },
          { src: "icons/icon-512.png", sizes: "512x512", type: "image/png" },
          { src: "icons/icon-maskable-512.png", sizes: "512x512", type: "image/png", purpose: "maskable" },
        ],
      },
      workbox: {
        globPatterns: ["**/*.{js,css,html,svg,png,woff2}"],
        globIgnores: ["executorch/**", "models/**"],
        maximumFileSizeToCacheInBytes: 3 * 1024 * 1024,
        runtimeCaching: [
          {
            urlPattern: ({ url }) => /\/(models|executorch)\//.test(url.pathname),
            handler: "CacheFirst",
            options: { cacheName: "shamba-offline-pack", expiration: { maxEntries: 20 } },
          },
        ],
      },
    }),
  ],
  test: { environment: "jsdom" },
});
