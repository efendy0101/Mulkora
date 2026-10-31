"use client";

import { AdminLayout } from "@/components/AdminLayout";
import { useQuery } from "@tanstack/react-query";
import { collection, query, where, getCountFromServer } from "firebase/firestore";
import { db } from "@/lib/firebase";

interface StatCardProps {
  label: string;
  value: number | string;
  color?: string;
}

function StatCard({ label, value, color }: StatCardProps) {
  return (
    <div className="stat-card">
      <div className="stat-value" style={color ? { color } : undefined}>
        {value}
      </div>
      <div className="stat-label">{label}</div>
    </div>
  );
}

function useDashboardStats() {
  return useQuery({
    queryKey: ["dashboard-stats"],
    queryFn: async () => {
      const [pendingPosts, reports, pendingRefs, totalUsers] = await Promise.all([
        getCountFromServer(
          query(collection(db, "moderationQueue"), where("status", "==", "pending"))
        ),
        getCountFromServer(
          query(collection(db, "reports"), where("status", "==", "new"))
        ),
        getCountFromServer(
          query(collection(db, "references"), where("status", "==", "pending_approval"))
        ),
        getCountFromServer(collection(db, "users")),
      ]);

      return {
        pendingPosts: pendingPosts.data().count,
        newReports: reports.data().count,
        pendingReferences: pendingRefs.data().count,
        totalUsers: totalUsers.data().count,
      };
    },
    refetchInterval: 30_000, // 30 saniyede bir yenile
  });
}

/**
 * SCR-ADMIN-001 — Genel Gösterge Paneli.
 * CLAUDE.md §21: İstatistikler ve bekleyen işlemler özeti.
 */
export default function DashboardPage() {
  const { data, isLoading, error } = useDashboardStats();

  return (
    <AdminLayout>
      <div className="admin-header">
        <h1 style={{ fontSize: "1.25rem", fontWeight: 700 }}>Gösterge Paneli</h1>
      </div>

      {isLoading ? (
        <div className="loading-center">
          <div className="spinner" />
        </div>
      ) : error ? (
        <div className="card" style={{ color: "var(--color-danger)" }}>
          İstatistikler yüklenemedi.
        </div>
      ) : (
        <>
          <div className="stats-grid">
            <StatCard
              label="Onay Bekleyen Gönderiler"
              value={data?.pendingPosts ?? 0}
              color={data?.pendingPosts ? "var(--color-warning)" : undefined}
            />
            <StatCard
              label="Yeni Şikayetler"
              value={data?.newReports ?? 0}
              color={data?.newReports ? "var(--color-danger)" : undefined}
            />
            <StatCard
              label="Onay Bekleyen Referanslar"
              value={data?.pendingReferences ?? 0}
            />
            <StatCard
              label="Toplam Kullanıcı"
              value={data?.totalUsers ?? 0}
            />
          </div>

          <div className="card">
            <h2 style={{ fontSize: "1rem", fontWeight: 600, marginBottom: 16 }}>
              Hızlı Erişim
            </h2>
            <div style={{ display: "flex", flexWrap: "wrap", gap: 12 }}>
              <a href="/moderation/posts" className="btn btn-primary">
                Gönderileri İncele
              </a>
              <a href="/moderation/reports" className="btn btn-danger">
                Şikayetleri İncele
              </a>
              <a href="/references/pending" className="btn btn-ghost">
                Referansları İncele
              </a>
              <a href="/users" className="btn btn-ghost">
                Kullanıcıları Yönet
              </a>
            </div>
          </div>
        </>
      )}
    </AdminLayout>
  );
}