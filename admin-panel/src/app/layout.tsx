import type { Metadata } from "next";
import { Providers } from "./providers";
import "./globals.css";

export const metadata: Metadata = {
  title: "Mülkora Yönetim Paneli",
  description: "Mülkora moderasyon ve yönetim paneli",
  robots: "noindex, nofollow", // Yönetim paneli arama motorlarına kapalı
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="tr">
      <body>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}