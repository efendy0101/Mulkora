"use client";

/**
 * SCR-ADMIN-010: Kullanıcı Listesi Sayfası
 *
 * CLAUDE.md §3: Admin tüm kullanıcı bilgilerine erişebilir ve rol atayabilir.
 * CLAUDE.md §3.5: Moderatörler kullanıcı listesini görebilir (salt okunur).
 * docs/11-firebase-collections.md — users koleksiyonu.
 */

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  collection,
  query,
  orderBy,
  limit,
  getDocs,
  where,
  startAfter,
  QueryDocumentSnapshot,
  DocumentData,
  Timestamp,
  QueryConstraint,
} from "firebase/firestore";
import { httpsCallable } from "firebase/functions";
import { db, functions } from "@/lib/firebase";
import { useAdminAuth } from "@/hooks/useAdminAuth";
import { AdminLayout } from "@/components/AdminLayout";
import toast from "react-hot-toast";
import { formatDistanceToNow } from "date-fns";
import { tr } from "date-fns/locale";

interface AppUser {
  id: string;
  displayName: string;
  username: string;
  email: string;
  role: "user" | "moderator" | "admin";
  isBlocked: boolean;
  emailVerified: boolean;
  landlordVerified?: boolean;
  postCount?: number;
  referenceCount?: number;
  createdAt: Timestamp;
}

const ROLE_LABELS: Record<string, { label: string; color: string }> = {
  admin: { label: "Admin", color: "#7c3aed" },
  moderator: { label: "Moderatör", color: "#1d4ed8" },
  user: { label: "Kullanıcı", color: "#374151" },
};

const PAGE_SIZE = 30;

export default function UsersPage() {
  const { user: adminUser } = useAdminAuth();
  const queryClient = useQueryClient();
  const [roleFilter, setRoleFilter] = useState<"all" | "user" | "moderator" | "admin">("all");
  const [searchQuery, setSearchQuery] = useState("");
  const [lastDoc, setLastDoc] = useState<QueryDocumentSnapshot<DocumentData> | null>(null);
  const [allUsers, setAllUsers] = useState<AppUser[]>([]);
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);

  const { isLoading, isFetching } = useQuery({
    queryKey: ["users", roleFilter, searchQuery],
    queryFn: async () => {
      const constraints: QueryConstraint[] = [
        orderBy("createdAt", "desc"),
        limit(PAGE_SIZE),
      ];

      if (roleFilter !== "all") {
        constraints.unshift(where("role", "==", roleFilter));
      }

      const q = query(collection(db, "users"), ...constraints);
      const snapshot = await getDocs(q);
      const users = snapshot.docs.map((d) => ({
        id: d.id,
        ...d.data(),
      })) as AppUser[];

      setAllUsers(users);
      setLastDoc(snapshot.docs[snapshot.docs.length - 1] ?? null);
      return users;
    },
    enabled: !!adminUser,
  });

  const loadMore = async () => {
    if (!lastDoc) return;
    const constraints: QueryConstraint[] = [
      orderBy("createdAt", "desc"),
      startAfter(lastDoc),
      limit(PAGE_SIZE),
    ];
    if (roleFilter !== "all") {
      constraints.unshift(where("role", "==", roleFilter));
    }
    const q = query(collection(db, "users"), ...constraints);
    const snapshot = await getDocs(q);
    const more = snapshot.docs.map((d) => ({
      id: d.id,
      ...d.data(),
    })) as AppUser[];
    setAllUsers((prev) => [...prev, ...more]);
    setLastDoc(snapshot.docs[snapshot.docs.length - 1] ?? null);
  };

  const setRoleMutation = useMutation({
    mutationFn: async ({
      targetUid,
      newRole,
    }: {
      targetUid: string;
      newRole: string;
    }) => {
      const setUserRole = httpsCallable(functions, "setUserRole");
      await setUserRole({ targetUid, newRole });
    },
    onSuccess: () => {
      toast.success("Kullanıcı rolü güncellendi.");
      setSelectedUserId(null);
      queryClient.invalidateQueries({ queryKey: ["users"] });
    },
    onError: (e: Error) => toast.error(`Hata: ${e.message}`),
  });

  const blockMutation = useMutation({
    mutationFn: async ({
      targetUid,
      block,
    }: {
      targetUid: string;
      block: boolean;
    }) => {
      const fn = httpsCallable(functions, block ? "blockUser" : "unblockUser");
      await fn({ targetUid });
    },
    onSuccess: (_, vars) => {
      toast.success(vars.block ? "Kullanıcı engellendi." : "Engel kaldırıldı.");
      queryClient.invalidateQueries({ queryKey: ["users"] });
    },
    onError: (e: Error) => toast.error(`Hata: ${e.message}`),
  });

  const isAdmin = adminUser?.role === "admin";

  const filteredUsers = searchQuery
    ? allUsers.filter(
        (u) =>
          u.displayName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
          u.username?.toLowerCase().includes(searchQuery.toLowerCase()) ||
          u.email?.toLowerCase().includes(searchQuery.toLowerCase())
      )
    : allUsers;

  return (
    <AdminLayout>
      <div style={{ maxWidth: 1200, margin: "0 auto" }}>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            marginBottom: 24,
          }}
        >
          <div>
            <h1 style={{ margin: 0, fontSize: 24 }}>Kullanıcılar</h1>
            <p style={{ margin: "4px 0 0", color: "#6b7280" }}>
              {allUsers.length} kullanıcı yüklendi
            </p>
          </div>
        </div>

        <div
          style={{
            display: "flex",
            gap: 12,
            marginBottom: 20,
            flexWrap: "wrap",
          }}
        >
          <input
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="İsim, kullanıcı adı veya e-posta ara..."
            style={{
              flex: 1,
              minWidth: 200,
              padding: "8px 12px",
              border: "1px solid #d1d5db",
              borderRadius: 8,
              fontSize: 14,
            }}
          />
          <select
            value={roleFilter}
            onChange={(e) => {
              setRoleFilter(e.target.value as typeof roleFilter);
              setAllUsers([]);
              setLastDoc(null);
            }}
            style={{
              padding: "8px 12px",
              border: "1px solid #d1d5db",
              borderRadius: 8,
              background: "white",
              fontSize: 14,
            }}
          >
            <option value="all">Tüm Roller</option>
            <option value="user">Kullanıcı</option>
            <option value="moderator">Moderatör</option>
            <option value="admin">Admin</option>
          </select>
        </div>

        {isLoading ? (
          <div style={{ textAlign: "center", padding: 48, color: "#6b7280" }}>
            Yükleniyor...
          </div>
        ) : (
          <>
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
                    {["Kullanıcı", "E-posta", "Rol", "Durum", "Kayıt", "İşlemler"].map(
                      (h) => (
                        <th
                          key={h}
                          style={{
                            padding: "10px 16px",
                            textAlign: "left",
                            fontSize: 11,
                            fontWeight: 700,
                            color: "#6b7280",
                            textTransform: "uppercase",
                            letterSpacing: "0.05em",
                          }}
                        >
                          {h}
                        </th>
                      )
                    )}
                  </tr>
                </thead>
                <tbody>
                  {filteredUsers.map((u, idx) => {
                    const roleInfo = ROLE_LABELS[u.role] ?? ROLE_LABELS.user;
                    const isSelected = selectedUserId === u.id;

                    return (
                      <>
                        <tr
                          key={u.id}
                          style={{
                            borderBottom:
                              idx < filteredUsers.length - 1
                                ? "1px solid #e5e7eb"
                                : "none",
                            backgroundColor: isSelected ? "#f9fafb" : "white",
                          }}
                        >
                          <td style={{ padding: "12px 16px" }}>
                            <div style={{ fontWeight: 600 }}>
                              {u.displayName || "—"}
                            </div>
                            <div style={{ fontSize: 12, color: "#6b7280" }}>
                              @{u.username || "—"}
                            </div>
                          </td>
                          <td style={{ padding: "12px 16px", fontSize: 13 }}>
                            {u.email}
                            {u.emailVerified && (
                              <span
                                style={{
                                  marginLeft: 4,
                                  fontSize: 11,
                                  color: "#16a34a",
                                }}
                              >
                                ✓
                              </span>
                            )}
                          </td>
                          <td style={{ padding: "12px 16px" }}>
                            <span
                              style={{
                                backgroundColor:
                                  roleInfo.color + "20",
                                color: roleInfo.color,
                                padding: "2px 8px",
                                borderRadius: 12,
                                fontSize: 12,
                                fontWeight: 600,
                              }}
                            >
                              {roleInfo.label}
                            </span>
                          </td>
                          <td style={{ padding: "12px 16px" }}>
                            {u.isBlocked ? (
                              <span
                                style={{
                                  color: "#dc2626",
                                  fontSize: 12,
                                  fontWeight: 600,
                                }}
                              >
                                Engellendi
                              </span>
                            ) : (
                              <span style={{ color: "#16a34a", fontSize: 12 }}>
                                Aktif
                              </span>
                            )}
                          </td>
                          <td
                            style={{
                              padding: "12px 16px",
                              fontSize: 12,
                              color: "#6b7280",
                            }}
                          >
                            {u.createdAt
                              ? formatDistanceToNow(u.createdAt.toDate(), {
                                  addSuffix: true,
                                  locale: tr,
                                })
                              : "—"}
                          </td>
                          <td style={{ padding: "12px 16px" }}>
                            {isAdmin && u.id !== adminUser?.uid && (
                              <div style={{ display: "flex", gap: 6 }}>
                                <button
                                  onClick={() =>
                                    setSelectedUserId(isSelected ? null : u.id)
                                  }
                                  style={{
                                    padding: "4px 10px",
                                    border: "1px solid #d1d5db",
                                    borderRadius: 6,
                                    background: isSelected ? "#f3f4f6" : "white",
                                    cursor: "pointer",
                                    fontSize: 12,
                                  }}
                                >
                                  Rol
                                </button>
                                <button
                                  onClick={() =>
                                    blockMutation.mutate({
                                      targetUid: u.id,
                                      block: !u.isBlocked,
                                    })
                                  }
                                  style={{
                                    padding: "4px 10px",
                                    border: `1px solid ${u.isBlocked ? "#bbf7d0" : "#fca5a5"}`,
                                    borderRadius: 6,
                                    background: u.isBlocked ? "#f0fdf4" : "#fef2f2",
                                    color: u.isBlocked ? "#16a34a" : "#dc2626",
                                    cursor: "pointer",
                                    fontSize: 12,
                                  }}
                                >
                                  {u.isBlocked ? "Engeli Kaldır" : "Engelle"}
                                </button>
                              </div>
                            )}
                          </td>
                        </tr>
                        {isSelected && isAdmin && (
                          <tr
                            key={u.id + "-expand"}
                            style={{ backgroundColor: "#f9fafb" }}
                          >
                            <td
                              colSpan={6}
                              style={{ padding: "12px 16px 16px" }}
                            >
                              <div style={{ fontSize: 13, marginBottom: 8, fontWeight: 600 }}>
                                Rol Değiştir:
                              </div>
                              <div style={{ display: "flex", gap: 8 }}>
                                {(["user", "moderator", "admin"] as const).map(
                                  (r) => (
                                    <button
                                      key={r}
                                      onClick={() =>
                                        setRoleMutation.mutate({
                                          targetUid: u.id,
                                          newRole: r,
                                        })
                                      }
                                      disabled={
                                        u.role === r ||
                                        setRoleMutation.isPending
                                      }
                                      style={{
                                        padding: "6px 14px",
                                        border: "1px solid #d1d5db",
                                        borderRadius: 6,
                                        background:
                                          u.role === r ? "#1d4ed8" : "white",
                                        color: u.role === r ? "white" : "#374151",
                                        cursor:
                                          u.role === r ||
                                          setRoleMutation.isPending
                                            ? "not-allowed"
                                            : "pointer",
                                        fontSize: 13,
                                        fontWeight: u.role === r ? 700 : 400,
                                      }}
                                    >
                                      {ROLE_LABELS[r].label}
                                    </button>
                                  )
                                )}
                              </div>
                            </td>
                          </tr>
                        )}
                      </>
                    );
                  })}
                </tbody>
              </table>
              {filteredUsers.length === 0 && (
                <div
                  style={{
                    padding: 32,
                    textAlign: "center",
                    color: "#6b7280",
                  }}
                >
                  Kullanıcı bulunamadı.
                </div>
              )}
            </div>

            {lastDoc && !searchQuery && (
              <div style={{ textAlign: "center", marginTop: 16 }}>
                <button
                  onClick={loadMore}
                  disabled={isFetching}
                  style={{
                    padding: "10px 24px",
                    border: "1px solid #d1d5db",
                    borderRadius: 8,
                    background: "white",
                    cursor: isFetching ? "not-allowed" : "pointer",
                    fontSize: 14,
                  }}
                >
                  {isFetching ? "Yükleniyor..." : "Daha Fazla Yükle"}
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </AdminLayout>
  );
}