/** @type {import('next').NextConfig} */
const nextConfig = {
  /**
   * Firebase Hosting static export yapılandırması.
   * firebase.json: "public": "admin-panel/out" → next build çıktısı bu dizine yazılır.
   * CLAUDE.md §30: Admin panel Firebase Hosting üzerinde yayımlanır.
   */
  output: "export",

  /**
   * Firebase Hosting rewrites kullandığı için trailingSlash gerekli değil.
   * Ancak static export'ta dinamik route yoktur; tüm sayfalar pre-render edilir.
   */
  trailingSlash: false,

  /**
   * Firebase Storage veya harici kaynaklardan gelen görseller için domain izinleri.
   * CLAUDE.md §7: Kullanıcı avatarları Firebase Storage'da saklanır.
   */
  images: {
    unoptimized: true, // static export sırasında next/image optimizasyonu devre dışı
  },
};

module.exports = nextConfig;