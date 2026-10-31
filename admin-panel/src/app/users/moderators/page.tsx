"use client";

/**
 * SCR-ADMIN-013: Moderatör Yönetimi Sayfası
 *
 * Yöneticilerin moderatör rolü atayıp kaldırabileceği panel sayfası.
 * Yalnızca admin rolündeki kullanıcılar erişebilir.
 *
 * CLAUDE.md §3.5 ve §21: Moderatör yönetimi — admin only
 * Firebase custom claims: role = "moderator" | "admin"
 * Cloud Function: setUserRole
 */

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  collection,
  query,
  where,
  orderBy,
  getDocs,
  Timestamp,
} from "firebase/firestore";
import { getFunctions, httpsCallable } from "firebase/functions";
import { db } from "@/lib/firebase";
import { useAdminAuth } from "@/hooks/useAdminAuth";
import { AdminLayout } from "@/components/AdminLayout";
import toast from "react-hot-toast";
import { formatDistanceToNow } from "date-fns";
import { tr } from "date-fns/locale";

interface ModeratorUser {
  id: string;
  uid: string;
  displayName?: string;
  username?: string;
  email: string;
  role: "moderator" | "admin";
  createdAt: Timestamp;
  lastLoginAt?: Timestamp;
}

export default function ModeratorsPage() {
  const { user } = useAdminAuth();
  const [selectedUser, setSelectedUser] = useState<ModeratorUser | null>(null);
  const [newModeratorEmail, setNewModeratorEmail] = useState("");
  const [isProcessing, setIsProcessing] = useState(false);
  const functions = getFunctions(undefined, "europe-west1");


  const {
    data: moderators = [],
    isLoading,
    refetch,
  } = useQuery({
    queryKey: ["moderators"],
    queryFn: async () => {
      const q = query(
        collection(db, "users"),
        where("role", "in", ["moderator", "admin"]),
        orderBy("createdAt", "asc")
      );
      const snapshot = await getDocs(q);
      return snapshot.docs.map((doc) => ({
        id: doc.id,
        ...doc.data(),
      })) as ModeratorUser[];
    },
    enabled: !!user && user.role === "admin",
  });

  // Sadece admin erişebilir
  if (user && user.role !== "admin") {
    return (
      <AdminLayout>
        <div
          style={{
            maxWidth: 480,
            margin: "80px auto",
            textAlign: "center",
            color: "#dc2626",
          }}
        >
          <div style={{ fontSize: 48, marginBottom: 16 }}>🔒</div>
          <h2>Erişim Reddedildi</h2>
          <p style={{ color: "#6b7280" }}>Bu sayfaya yalnızca yöneticiler erişebilir.</p>
        </div>
      </AdminLayout>
    );
  }


  const handlePromoteToModerator = async (email: string) => {
    if (!email.trim()) {
      toast.error("E-posta adresi zorunludur.");
      return;
    }
    setIsProcessing(true);
    try {
      const setUserRole = httpsCallable(functions, "setUserRole");
      await setUserRole({ email: email.trim(), role: "moderator" });
      toast.success("Kullanıcı moderatör olarak atandı.");
      setNewModeratorEmail("");
      refetch();
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : "Bilinmeyen hata";
      toast.error(`Hata: ${message}`);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleRemoveModerator = async (uid: string, username: string) => {
    if (!confirm(`@${username} kullanıcısının moderatörlük yetkisi kaldırılacak. Onaylıyor musunuz?`)) return;
    setIsProcessing(true);
    try {
      const setUserRole = httpsCallable(functions, "setUserRole");
      await setUserRole({ uid, role: "user" });
      toast.success("Moderatörlük yetkisi kaldırıldı.");
      setSelectedUser(null);
      refetch();
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : "Bilinmeyen hata";
      toast.error(`Hata: ${message}`);
    } finally {
      setIsProcessing(false);
    }
  };

  const handlePromoteToAdmin = async (uid: string, username: string) => {
    if (!confirm(`@${username} kullanıcısı yönetici yapılacak. Bu işlem geri alınamaz. Onaylıyor musunuz?`)) return;
    setIsProcessing(true);
    try {
      const setUserRole = httpsCallable(functions, "setUserRole");
      await setUserRole({ uid, role: "admin" });
      toast.success("Kullanıcı yönetici yapıldı.");
      setSelectedUser(null);
      refetch();
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : "Bilinmeyen hata";
      toast.error(`Hata: ${message}`);
    } finally {
      setIsProcessing(false);
    }
  };

  const adminList = moderators.filter((m) => m.role === "admin");
  const moderatorList = moderators.filter((m) => m.role === "moderator");

  return (
    <AdminLayout>
      <div style={{ maxWidth: 880, margin: "0 auto" }}>
        <div style={{ marginBottom: 28 }}>
          <h1 style={{ margin: 0, fontSize: 24 }}>Moderatör Yönetimi</h1>
          <p style={{ margin: "4px 0 0", color: "#6b7280" }}>
            Moderatör atama, yetki kaldırma ve yönetici terfi işlemleri.
          </p>
        </div>

        {/* Yeni Moderatör Ekle */}
        <div
          style={{
            backgroundColor: "white",
            border: "1px solid #e5e7eb",
            borderRadius: 12,
            padding: 20,
            marginBottom: 28,
          }}
        >
          <h2 style={{ margin: "0 0 12px", fontSize: 16 }}>Yeni Moderatör Ata</h2>
          <div style={{ display: "flex", gap: 8 }}>
            <input
              type="email"
              value={newModeratorEmail}
              onChange={(e) => setNewModeratorEmail(e.target.value)}
              placeholder="kullanici@ornek.com"
              style={{
                flex: 1,
                padding: "8px 12px",
                borderRadius: 8,
                border: "1px solid #d1d5db",
                fontSize: 14,
              }}
              onKeyDown={(e) => {
                if (e.key === "Enter") handlePromoteToModerator(newModeratorEmail);
              }}
            />
            <button
              onClick={() => handlePromoteToModerator(newModeratorEmail)}
              disabled={isProcessing || !newModeratorEmail.trim()}
              style={{
                padding: "8px 18px",
                backgroundColor: "#2563eb",
                color: "white",
                border: "none",
                borderRadius: 8,
                cursor: isProcessing ? "not-allowed" : "pointer",
                fontWeight: 600,
                fontSize: 14,
              }}
            >
              {isProcessing ? "İşleniyor..." : "Ata"}
            </button>
          </div>
          <p style={{ margin: "8px 0 0", fontSize: 12, color: "#9ca3af" }}>
            Kullanıcının sistemde kayıtlı e-posta adresini girin.
          </p>
        </div>

        {isLoading ? (
          <div style={{ textAlign: "center", padding: 48, color: "#6b7280" }}>
            Yükleniyor...
          </div>
        ) : (
          <>
            {/* Yöneticiler */}
            {adminList.length > 0 && (
              <div style={{ marginBottom: 24 }}>
                <h2 style={{ fontSize: 15, color: "#374151", marginBottom: 10 }}>
                  Yöneticiler ({adminList.length})
                </h2>
                <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
                  {adminList.map((admin) => (
                    <div
                      key={admin.id}
                      style={{
                        backgroundColor: "#fff7ed",
                        border: "1px solid #fed7aa",
                        borderRadius: 10,
                        padding: 14,
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "center",
                      }}
                    >
                      <div>
                        <div style={{ fontWeight: 600 }}>
                          {admin.displayName ?? admin.username ?? admin.email}
                          {admin.id === user?.uid && (
                            <span
                              style={{
                                marginLeft: 8,
                                fontSize: 11,
                                backgroundColor: "#fed7aa",
                                color: "#9a3412",
                                padding: "1px 6px",
                                borderRadius: 10,
                              }}
                            >
                              Siz
                            </span>
                          )}
                        </div>
                        <div style={{ fontSize: 12, color: "#6b7280", marginTop: 2 }}>
                          {admin.email}
                          {admin.username && ` · @${admin.username}`}
                        </div>
                        {admin.createdAt && (
                          <div style={{ fontSize: 11, color: "#9ca3af", marginTop: 2 }}>
                            {formatDistanceToNow(admin.createdAt.toDate(), {
                              addSuffix: true,
                              locale: tr,
                            })}
                          </div>
                        )}
                      </div>
                      <span
                        style={{
                          backgroundColor: "#fed7aa",
                          color: "#9a3412",
                          padding: "3px 10px",
                          borderRadius: 12,
                          fontSize: 12,
                          fontWeight: 600,
                        }}
                      >
                        Admin
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Moderatörler */}
            <div>
              <h2 style={{ fontSize: 15, color: "#374151", marginBottom: 10 }}>
                Moderatörler ({moderatorList.length})
              </h2>
              {moderatorList.length === 0 ? (
                <div
                  style={{
                    backgroundColor: "#f9fafb",
                    border: "1px solid #e5e7eb",
                    borderRadius: 10,
                    padding: 32,
                    textAlign: "center",
                    color: "#9ca3af",
                    fontSize: 14,
                  }}
                >
                  Henüz moderatör atanmamış.
                </div>
              ) : (
                <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
                  {moderatorList.map((mod) => (
                    <div
                      key={mod.id}
                      style={{
                        backgroundColor: "white",
                        border: "1px solid #e5e7eb",
                        borderRadius: 10,
                        padding: 14,
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "center",
                      }}
                    >
                      <div>
                        <div style={{ fontWeight: 600 }}>
                          {mod.displayName ?? mod.username ?? mod.email}
                        </div>
                        <div style={{ fontSize: 12, color: "#6b7280", marginTop: 2 }}>
                          {mod.email}
                          {mod.username && ` · @${mod.username}`}
                        </div>
                        {mod.createdAt && (
                          <div style={{ fontSize: 11, color: "#9ca3af", marginTop: 2 }}>
                            {formatDistanceToNow(mod.createdAt.toDate(), {
                              addSuffix: true,
                              locale: tr,
                            })}
                          </div>
                        )}
                      </div>
                      <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                        <span
                          style={{
                            backgroundColor: "#eff6ff",
                            color: "#1d4ed8",
                            padding: "3px 10px",
                            borderRadius: 12,
                            fontSize: 12,
                            fontWeight: 600,
                          }}
                        >
                          Moderatör
                        </span>
                        <button
                          onClick={() => setSelectedUser(mod)}
                          style={{
                            padding: "4px 10px",
                            border: "1px solid #d1d5db",
                            borderRadius: 6,
                            background: "white",
                            cursor: "pointer",
                            fontSize: 12,
                          }}
                        >
                          İşlem
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </>
        )}
      </div>

      {/* İşlem Modalı */}
      {selectedUser && (
        <div
          style={{
            position: "fixed",
            inset: 0,
            backgroundColor: "rgba(0,0,0,0.5)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 50,
          }}
        >
          <div
            style={{
              backgroundColor: "white",
              borderRadius: 12,
              padding: 24,
              maxWidth: 400,
              width: "90%",
            }}
          >
            <h2 style={{ margin: "0 0 4px", fontSize: 18 }}>Moderatör İşlemleri</h2>
            <p style={{ margin: "0 0 20px", color: "#6b7280", fontSize: 14 }}>
              <strong>@{selectedUser.username ?? selectedUser.email}</strong>
            </p>

            <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
              <button
                onClick={() =>
                  handlePromoteToAdmin(
                    selectedUser.uid,
                    selectedUser.username ?? selectedUser.email
                  )
                }
                disabled={isProcessing}
                style={{
                  padding: "10px",
                  backgroundColor: "#fff7ed",
                  color: "#9a3412",
                  border: "1px solid #fed7aa",
                  borderRadius: 8,
                  cursor: "pointer",
                  fontWeight: 600,
                  fontSize: 14,
                }}
              >
                Yönetici Yap
              </button>
              <button
                onClick={() =>
                  handleRemoveModerator(
                    selectedUser.uid,
                    selectedUser.username ?? selectedUser.email
                  )
                }
                disabled={isProcessing}
                style={{
                  padding: "10px",
                  backgroundColor: "#fef2f2",
                  color: "#dc2626",
                  border: "1px solid #fca5a5",
                  borderRadius: 8,
                  cursor: "pointer",
                  fontWeight: 600,
                  fontSize: 14,
                }}
              >
                Yetkiyi Kaldır
              </button>
              <button
                onClick={() => setSelectedUser(null)}
                style={{
                  padding: "10px",
                  border: "1px solid #d1d5db",
                  borderRadius: 8,
                  background: "white",
                  cursor: "pointer",
                  fontSize: 14,
                }}
              >
                İptal
              </button>
            </div>
          </div>
        </div>
      )}
    </AdminLayout>
  );
}