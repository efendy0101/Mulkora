"use client";

/**
 * SCR-ADMIN-021: İstatistikler Sayfası
 *
 * Platform genelindeki kullanıcı, gönderi, yorum, referans ve satın alma
 * istatistiklerini görselleştiren yönetim paneli sayfası.
 *
 * CLAUDE.md §21: İstatistikler — admin only
 * docs/22-development-phases.md §12
 */

import { useQuery } from "@tanstack/react-query";
import {
  collection,
  query,
  where,
  getCountFromServer,
  orderBy,
  limit,
  getDocs,
  Timestamp,
} from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAdminAuth } from "@/hooks/useAdminAuth";
import { AdminLayout } from "@/components/AdminLayout";

interface StatCardProps {
  label: string;
  value: number | string;
  subtitle?: string;
  color?: string;
  bgColor?: string;
}

function StatCard({ label, value, subtitle, color = "#111827", bgColor = "white" }: StatCardProps) {
  return (
    <div
      style={{
        backgroundColor: bgColor,
        border: "1px solid #e5e7eb",
        borderRadius: 12,
        padding: 20,
      }}
    >
      <div style={{ fontSize: 28, fontWeight: 700, color }}>{value}</div>
      <div style={{ fontSize: 14, fontWeight: 600, color: "#374151", marginTop: 2 }}>{label}</div>
      {subtitle && (
        <div style={{ fontSize: 12, color: "#9ca3af", marginTop: 4 }}>{subtitle}</div>
      )}
    </div>
  );
}

interface PlatformStats {
  totalUsers: number;
  totalPosts: number;
  approvedPosts: number;
  pendingPosts: number;
  rejectedPosts: number;
  totalComments: number;
  totalReferences: number;
  approvedReferences: number;
  totalReports: number;
  openReports: number;
  totalContracts: number;
  adsRemoved: number;
}

function usePlatformStats() {
  return useQuery<PlatformStats>({
    queryKey: ["platform-stats"],
    queryFn: async () => {
      const [
        totalUsers,
        totalPosts,
        approvedPosts,
        pendingPosts,
        rejectedPosts,
        totalComments,
        totalReferences,
        approvedReferences,
        totalReports,
        openReports,
        totalContracts,
        adsRemoved,
      ] = await Promise.all([
        getCountFromServer(collection(db, "users")),
        getCountFromServer(collection(db, "posts")),
        getCountFromServer(query(collection(db, "posts"), where("status", "==", "approved"))),
        getCountFromServer(
          query(collection(db, "moderationQueue"), where("status", "==", "pending"))
        ),
        getCountFromServer(
          query(collection(db, "moderationQueue"), where("status", "==", "rejected"))
        ),
        getCountFromServer(collection(db, "comments")),
        getCountFromServer(collection(db, "references")),
        getCountFromServer(
          query(collection(db, "references"), where("status", "==", "approved"))
        ),
        getCountFromServer(collection(db, "reports")),
        getCountFromServer(
          query(collection(db, "reports"), where("status", "in", ["new", "reviewing"]))
        ),
        getCountFromServer(collection(db, "contracts")),
        getCountFromServer(
          query(collection(db, "purchases"), where("productId", "==", "remove_ads_lifetime"))
        ),
      ]);

      return {
        totalUsers: totalUsers.data().count,
        totalPosts: totalPosts.data().count,
        approvedPosts: approvedPosts.data().count,
        pendingPosts: pendingPosts.data().count,
        rejectedPosts: rejectedPosts.data().count,
        totalComments: totalComments.data().count,
        totalReferences: totalReferences.data().count,
        approvedReferences: approvedReferences.data().count,
        totalReports: totalReports.data().count,
        openReports: openReports.data().count,
        totalContracts: totalContracts.data().count,
        adsRemoved: adsRemoved.data().count,
      };
    },
    staleTime: 60_000, // 1 dakika önbellekle
  });
}

interface RecentUser {
  id: string;
  displayName?: string;
  username?: string;
  email?: string;
  role?: string;
  createdAt: Timestamp;
}

function useRecentUsers() {
  return useQuery<RecentUser[]>({
    queryKey: ["recent-users"],
    queryFn: async () => {
      const q = query(collection(db, "users"), orderBy("createdAt", "desc"), limit(5));
      const snap = await getDocs(q);
      return snap.docs.map((doc) => ({ id: doc.id, ...doc.data() })) as RecentUser[];
    },
  });
}

export default function StatisticsPage() {
  const { user } = useAdminAuth();
  const { data: stats, isLoading: statsLoading } = usePlatformStats();
  const { data: recentUsers } = useRecentUsers();

  // Sadece admin erişebilir
  if (user && user.role !== "admin") {
    return (
      <AdminLayout>
        <div style={{ maxWidth: 480, margin: "80px auto", textAlign: "center", color: "#dc2626" }}>
          <div style={{ fontSize: 48, marginBottom: 16 }}>🔒</div>
          <h2>Erişim Reddedildi</h2>
          <p style={{ color: "#6b7280" }}>Bu sayfaya yalnızca yöneticiler erişebilir.</p>
        </div>
      </AdminLayout>
    );
  }

  return (
    <AdminLayout>
      <div style={{ maxWidth: 1100, margin: "0 auto" }}>
        <div style={{ marginBottom: 28 }}>
          <h1 style={{ margin: 0, fontSize: 24 }}>İstatistikler</h1>
          <p style={{ margin: "4px 0 0", color: "#6b7280" }}>Platform geneli istatistikler</p>
        </div>

        {statsLoading ? (
          <div style={{ textAlign: "center", padding: 64, color: "#6b7280" }}>
            İstatistikler yükleniyor...
          </div>
        ) : (
          <>
            {/* Kullanıcılar */}
            <div style={{ marginBottom: 24 }}>
              <h2 style={{ fontSize: 14, color: "#6b7280", marginBottom: 10, textTransform: "uppercase", letterSpacing: "0.05em" }}>
                Kullanıcılar
              </h2>
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "repeat(auto-fill, minmax(180px, 1fr))",
                  gap: 12,
                }}
              >
                <StatCard
                  label="Toplam Kullanıcı"
                  value={stats?.totalUsers ?? 0}
                  bgColor="#eff6ff"
                  color="#1d4ed8"
                />
                <StatCard
                  label="Reklam Kaldıran"
                  value={stats?.adsRemoved ?? 0}
                  subtitle="Ömür boyu satın alma"
                  bgColor="#f0fdf4"
                  color="#15803d"
                />
              </div>
            </div>

            {/* Gönderiler */}
            <div style={{ marginBottom: 24 }}>
              <h2 style={{ fontSize: 14, color: "#6b7280", marginBottom: 10, textTransform: "uppercase", letterSpacing: "0.05em" }}>
                Gönderiler
              </h2>
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "repeat(auto-fill, minmax(180px, 1fr))",
                  gap: 12,
                }}
              >
                <StatCard label="Toplam Gönderi" value={stats?.totalPosts ?? 0} />
                <StatCard
                  label="Yayımlanan"
                  value={stats?.approvedPosts ?? 0}
                  bgColor="#f0fdf4"
                  color="#15803d"
                />
                <StatCard
                  label="İncelemede"
                  value={stats?.pendingPosts ?? 0}
                  bgColor="#fffbeb"
                  color="#b45309"
                />
                <StatCard
                  label="Reddedilen"
                  value={stats?.rejectedPosts ?? 0}
                  bgColor="#fef2f2"
                  color="#dc2626"
                />
                <StatCard label="Toplam Yorum" value={stats?.totalComments ?? 0} />
              </div>
            </div>

            {/* Onay Oranı */}
            {stats && stats.totalPosts > 0 && (
              <div style={{ marginBottom: 24 }}>
                <div
                  style={{
                    backgroundColor: "white",
                    border: "1px solid #e5e7eb",
                    borderRadius: 12,
                    padding: 20,
                  }}
                >
                  <div style={{ fontSize: 14, fontWeight: 600, color: "#374151", marginBottom: 12 }}>
                    Gönderi Onay Oranı
                  </div>
                  <div style={{ display: "flex", gap: 4, height: 20, borderRadius: 10, overflow: "hidden" }}>
                    {(() => {
                      const total = stats.totalPosts;
                      const approvedPct = Math.round((stats.approvedPosts / total) * 100);
                      const pendingPct = Math.round((stats.pendingPosts / total) * 100);
                      const rejectedPct = 100 - approvedPct - pendingPct;
                      return (
                        <>
                          <div style={{ flex: approvedPct, backgroundColor: "#22c55e" }} title={`Onaylanan: %${approvedPct}`} />
                          <div style={{ flex: pendingPct, backgroundColor: "#f59e0b" }} title={`Bekleyen: %${pendingPct}`} />
                          <div style={{ flex: Math.max(rejectedPct, 0), backgroundColor: "#ef4444" }} title={`Reddedilen: %${rejectedPct}`} />
                        </>
                      );
                    })()}
                  </div>
                  <div style={{ display: "flex", gap: 16, marginTop: 8, fontSize: 12, color: "#6b7280" }}>
                    <span style={{ display: "flex", alignItems: "center", gap: 4 }}>
                      <span style={{ width: 10, height: 10, backgroundColor: "#22c55e", borderRadius: 2, display: "inline-block" }} />
                      Onaylandı %{Math.round((stats.approvedPosts / stats.totalPosts) * 100)}
                    </span>
                    <span style={{ display: "flex", alignItems: "center", gap: 4 }}>
                      <span style={{ width: 10, height: 10, backgroundColor: "#f59e0b", borderRadius: 2, display: "inline-block" }} />
                      Beklemede %{Math.round((stats.pendingPosts / stats.totalPosts) * 100)}
                    </span>
                    <span style={{ display: "flex", alignItems: "center", gap: 4 }}>
                      <span style={{ width: 10, height: 10, backgroundColor: "#ef4444", borderRadius: 2, display: "inline-block" }} />
                      Reddedildi %{Math.round((stats.rejectedPosts / stats.totalPosts) * 100)}
                    </span>
                  </div>
                </div>
              </div>
            )}

            {/* Referanslar & Şikayetler & Sözleşmeler */}
            <div style={{ marginBottom: 24 }}>
              <h2 style={{ fontSize: 14, color: "#6b7280", marginBottom: 10, textTransform: "uppercase", letterSpacing: "0.05em" }}>
                Diğer
              </h2>
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "repeat(auto-fill, minmax(180px, 1fr))",
                  gap: 12,
                }}
              >
                <StatCard label="Toplam Referans" value={stats?.totalReferences ?? 0} />
                <StatCard
                  label="Onaylı Referans"
                  value={stats?.approvedReferences ?? 0}
                  bgColor="#f0fdf4"
                  color="#15803d"
                />
                <StatCard label="Toplam Şikayet" value={stats?.totalReports ?? 0} />
                <StatCard
                  label="Açık Şikayet"
                  value={stats?.openReports ?? 0}
                  bgColor={stats && stats.openReports > 0 ? "#fef2f2" : "white"}
                  color={stats && stats.openReports > 0 ? "#dc2626" : "#111827"}
                />
                <StatCard label="Toplam Sözleşme" value={stats?.totalContracts ?? 0} />
              </div>
            </div>

            {/* Son Kayıt Olan Kullanıcılar */}
            {recentUsers && recentUsers.length > 0 && (
              <div>
                <h2 style={{ fontSize: 14, color: "#6b7280", marginBottom: 10, textTransform: "uppercase", letterSpacing: "0.05em" }}>
                  Son Kayıt Olan Kullanıcılar
                </h2>
                <div
                  style={{
                    backgroundColor: "white",
                    border: "1px solid #e5e7eb",
                    borderRadius: 12,
                    overflow: "hidden",
                  }}
                >
                  {recentUsers.map((u, idx) => (
                    <div
                      key={u.id}
                      style={{
                        padding: "12px 16px",
                        borderBottom: idx < recentUsers.length - 1 ? "1px solid #f3f4f6" : "none",
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "center",
                      }}
                    >
                      <div>
                        <div style={{ fontWeight: 600, fontSize: 14 }}>
                          {u.displayName ?? u.username ?? "—"}
                        </div>
                        <div style={{ fontSize: 12, color: "#6b7280" }}>
                          {u.email ?? ""}
                          {u.username && ` · @${u.username}`}
                        </div>
                      </div>
                      <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                        {u.role && (
                          <span
                            style={{
                              backgroundColor: "#f3f4f6",
                              color: "#374151",
                              padding: "2px 8px",
                              borderRadius: 10,
                              fontSize: 11,
                            }}
                          >
                            {u.role}
                          </span>
                        )}
                        {u.createdAt && (
                          <span style={{ fontSize: 11, color: "#9ca3af" }}>
                            {u.createdAt.toDate().toLocaleDateString("tr-TR")}
                          </span>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </AdminLayout>
  );
}