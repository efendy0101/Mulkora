/** @type {import('next').NextConfig} */
const nextConfig = {
  // Firebase Hosting için statik export
  // `firebase.json` → "public": "admin-panel/out"
  output: "export",
  // Trailing slash — Hosting SPA rewrites ile uyumlu
  trailingSlash: true,
  // Görsel optimizasyonu statik export'ta disabled olmalı
  images: {
    unoptimized: true,
  },
};

export default nextConfig;