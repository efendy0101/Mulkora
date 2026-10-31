"use client";

/**
 * SCR-ADMIN-006: Engellenen Kullanıcılar Sayfası
 *
 * CLAUDE.md §3.5: Yönetici kullanıcıları geçici veya kalıcı olarak engelleyebilir.
 * docs/11-firebase-collections.md — users koleksiyonu (isBlocked: true).
 */

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  collection,
  query,
  where,
  orderBy,
  limit,
  getDocs,
  Timestamp,
} from "firebase/firestore";
import { httpsCallable } from "firebase/functions";
import { db, functions } from "@/lib/firebase";
import { useAdminAuth } from "@/hooks/useAdminAuth";
import { AdminLayout } from "@/components/AdminLayout";
import toast from "react-hot-toast";
import { formatDistanceToNow } from "date-fns";
import { tr } from "date-fns/locale";

interface BlockedUser {
  id: string;
  displayName: string;
  username: string;
  email: string;
  role: string;
  isBlocked: boolean;
  blockedAt?: Timestamp;
  blockedReason?: string;
  createdAt: Timestamp;
}

export default function BlockedUsersPage() {
  const { user: adminUser } = useAdminAuth();
  const queryClient = useQueryClient();

  const { data: blockedUsers = [], isLoading } = useQuery({
    queryKey: ["blocked-users"],
    queryFn: async () => {
      const q = query(
        collection(db, "users"),
        where("isBlocked", "==", true),
        orderBy("blockedAt", "desc"),
        limit(100)
      );
      const snapshot = await getDocs(q);
      return snapshot.docs.map((doc) => ({
        id: doc.id,
        ...doc.data(),
      })) as BlockedUser[];
    },
    enabled: !!adminUser,
  });

  const unblockMutation = useMutation({
    mutationFn: async (uid: string) => {
      const unblockUser = httpsCallable(functions, "unblockUser");
      await unblockUser({ targetUid: uid });
    },
    onSuccess: () => {
      toast.success("Kullanıcı engeli kaldırıldı.");
      queryClient.invalidateQueries({ queryKey: ["blocked-users"] });
    },
    onError: (error: Error) => {
      toast.error(`Hata: ${error.message}`);
    },
  });

  return (
    <AdminLayout>
      <div style={{ maxWidth: 960, margin: "0 auto" }}>
        <div style={{ marginBottom: 24 }}>
          <h1 style={{ margin: 0, fontSize: 24 }}>Engellenen Kullanıcılar</h1>
          <p style={{ margin: "4px 0 0", color: "#6b7280" }}>
            {blockedUsers.length} kullanıcı engellendi
          </p>
        </div>

        {isLoading ? (
          <div style={{ textAlign: "center", padding: 48, color: "#6b7280" }}>
            Yükleniyor...
          </div>
        ) : blockedUsers.length === 0 ? (
          <div
            style={{
              backgroundColor: "#f0fdf4",
              border: "1px solid #bbf7d0",
              borderRadius: 12,
              padding: 48,
              textAlign: "center",
              color: "#15803d",
            }}
          >
            <div style={{ fontSize: 48, marginBottom: 16 }}>✓</div>
            <div style={{ fontSize: 18, fontWeight: 600 }}>
              Engellenen kullanıcı yok
            </div>
          </div>
        ) : (
          <div
            style={{
              backgroundColor: "white",
              border: "1px solid #e5e7eb",
              borderRadius: 12,
              overflow: "hidden",
            }}
          >
            <table style={{ width: "100%", borderCollapse: "collapse" }}>
              <thead>
                <tr
                  style={{
                    backgroundColor: "#f9fafb",
                    borderBottom: "1px solid #e5e7eb",
                  }}
                >
                  {[
                    "Kullanıcı",
                    "E-posta",
                    "Engellenme Tarihi",
                    "Neden",
                    "İşlem",
                  ].map((h) => (
                    <th
                      key={h}
                      style={{
                        padding: "12px 16px",
                        textAlign: "left",
                        fontSize: 12,
                        fontWeight: 600,
                        color: "#6b7280",
                        textTransform: "uppercase",
                        letterSpacing: "0.05em",
                      }}
                    >
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {blockedUsers.map((u, idx) => (
                  <tr
                    key={u.id}
                    style={{
                      borderBottom:
                        idx < blockedUsers.length - 1
                          ? "1px solid #e5e7eb"
                          : "none",
                    }}
                  >
                    <td style={{ padding: "12px 16px" }}>
                      <div style={{ fontWeight: 600 }}>{u.displayName}</div>
                      <div style={{ fontSize: 13, color: "#6b7280" }}>
                        @{u.username}
                      </div>
                    </td>
                    <td style={{ padding: "12px 16px", fontSize: 13 }}>
                      {u.email}
                    </td>
                    <td style={{ padding: "12px 16px", fontSize: 13 }}>
                      {u.blockedAt
                        ? formatDistanceToNow(u.blockedAt.toDate(), {
                            addSuffix: true,
                            locale: tr,
                          })
                        : "—"}
                    </td>
                    <td
                      style={{
                        padding: "12px 16px",
                        fontSize: 13,
                        color: "#6b7280",
                        maxWidth: 200,
                      }}
                    >
                      {u.blockedReason ?? "—"}
                    </td>
                    <td style={{ padding: "12px 16px" }}>
                      {adminUser?.role === "admin" && (
                        <button
                          onClick={() => unblockMutation.mutate(u.id)}
                          disabled={unblockMutation.isPending}
                          style={{
                            padding: "6px 12px",
                            backgroundColor: "#16a34a",
                            color: "white",
                            border: "none",
                            borderRadius: 6,
                            cursor: unblockMutation.isPending
                              ? "not-allowed"
                              : "pointer",
                            fontSize: 12,
                            fontWeight: 600,
                          }}
                        >
                          Engeli Kaldır
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </AdminLayout>
  );
}