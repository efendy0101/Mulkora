"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useAdminAuth } from "@/hooks/useAdminAuth";
import { signOut } from "@/lib/auth";
import { useEffect } from "react";
import toast from "react-hot-toast";
import { clsx } from "clsx";

const NAV_ITEMS = [
  {
    section: "Moderasyon",
    items: [
      { href: "/dashboard", label: "Gösterge Paneli" },
      { href: "/moderation/posts", label: "Onay Bekleyen Gönderiler" },
      { href: "/moderation/published", label: "Yayımlanan Gönderiler" },
      { href: "/moderation/rejected", label: "Reddedilen Gönderiler" },
      { href: "/moderation/comments", label: "Yorum Yönetimi" },
      { href: "/moderation/reports", label: "Şikayetler" },
    ],
  },
  {
    section: "Referanslar",
    items: [
      { href: "/references/pending", label: "Onay Bekleyen Referanslar" },
      { href: "/references/responses", label: "Referans Cevapları" },
      { href: "/references/appeals", label: "Referans İtirazları" },
    ],
  },
  {
    section: "Kullanıcılar",
    items: [
      { href: "/users", label: "Kullanıcılar" },
      { href: "/users/blocked", label: "Engellenen Kullanıcılar" },
      { href: "/users/moderators", label: "Moderatör Yönetimi", adminOnly: true },
    ],
  },
  {
    section: "Sistem",
    items: [
      { href: "/categories", label: "Kategoriler", adminOnly: true },
      { href: "/banned-terms", label: "Yasaklı Kelimeler", adminOnly: true },
      { href: "/contract-templates", label: "Sözleşme Şablonları", adminOnly: true },
      { href: "/audit-logs", label: "Denetim Kayıtları", adminOnly: true },
      { href: "/notifications/send", label: "Bildirim Gönder", adminOnly: true },
      { href: "/settings/ads", label: "Reklam Ayarları", adminOnly: true },
      { href: "/statistics", label: "İstatistikler", adminOnly: true },
    ],
  },
];

/**
 * Yönetim paneli ana yerleşim bileşeni.
 *
 * CLAUDE.md §21: Moderatörler yalnızca içerik moderasyon bölümlerine erişebilir.
 * Adminler tüm bölümlere erişebilir.
 */
export function AdminLayout({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAdminAuth();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (!loading && !user) {
      router.replace("/login");
    }
  }, [user, loading, router]);

  if (loading || !user) {
    return (
      <div className="loading-center" style={{ minHeight: "100vh" }}>
        <div className="spinner" />
      </div>
    );
  }

  async function handleSignOut() {
    try {
      await signOut();
      router.replace("/login");
    } catch {
      toast.error("Çıkış yapılırken hata oluştu");
    }
  }

  return (
    <div className="admin-layout">
      {/* Sidebar */}
      <aside className="admin-sidebar">
        <div style={{ padding: "16px 20px 24px", borderBottom: "1px solid var(--color-border)" }}>
          <div style={{ fontWeight: 700, fontSize: "1.1rem", color: "var(--color-primary)" }}>
            Mülkora
          </div>
          <div style={{ fontSize: "0.75rem", color: "var(--color-text-secondary)", marginTop: 2 }}>
            Yönetim Paneli
          </div>
        </div>

        <nav style={{ paddingTop: 8 }}>
          {NAV_ITEMS.map(({ section, items }) => (
            <div key={section}>
              <div className="nav-section-title">{section}</div>
              {items.map((item) => {
                // adminOnly öğeleri moderatörlerden gizle
                if ("adminOnly" in item && item.adminOnly && user.role !== "admin") {
                  return null;
                }
                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    className={clsx("nav-item", pathname === item.href && "active")}
                  >
                    {item.label}
                  </Link>
                );
              })}
            </div>
          ))}
        </nav>

        {/* Kullanıcı bilgisi */}
        <div
          style={{
            position: "absolute",
            bottom: 0,
            left: 0,
            right: 0,
            padding: "12px 20px",
            borderTop: "1px solid var(--color-border)",
            background: "var(--color-surface)",
          }}
        >
          <div style={{ fontSize: "0.8rem", fontWeight: 600, marginBottom: 2 }}>
            {user.displayName ?? user.email}
          </div>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
            <span className={clsx("badge", user.role === "admin" ? "badge-admin" : "badge-moderator")}>
              {user.role === "admin" ? "Admin" : "Moderatör"}
            </span>
            <button
              className="btn btn-ghost"
              style={{ padding: "4px 8px", fontSize: "0.75rem" }}
              onClick={handleSignOut}
            >
              Çıkış
            </button>
          </div>
        </div>
      </aside>

      {/* Ana içerik */}
      <main className="admin-main">{children}</main>
    </div>
  );
}