"use client";

/**
 * SCR-ADMIN-009: Denetim Kayıtları Sayfası
 *
 * CLAUDE.md §3.5: Tüm moderatör ve admin işlemleri kayıt altına alınmalıdır.
 * docs/11-firebase-collections.md — auditLogs koleksiyonu.
 * Yalnızca adminler erişebilir.
 */

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  collection,
  query,
  orderBy,
  limit,
  where,
  getDocs,
  startAfter,
  QueryDocumentSnapshot,
  DocumentData,
  Timestamp,
  QueryConstraint,
} from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAdminAuth } from "@/hooks/useAdminAuth";
import { AdminLayout } from "@/components/AdminLayout";
import { formatDistanceToNow, format } from "date-fns";
import { tr } from "date-fns/locale";

interface AuditLog {
  id: string;
  adminUid: string;
  adminEmail?: string;
  action: string;
  targetType: "post" | "user" | "comment" | "reference" | "report" | "system";
  targetId: string;
  description: string;
  previousState?: Record<string, unknown>;
  newState?: Record<string, unknown>;
  createdAt: Timestamp;
}

const ACTION_LABELS: Record<string, { label: string; color: string }> = {
  approve_post: { label: "Gönderi Onaylandı", color: "#16a34a" },
  reject_post: { label: "Gönderi Reddedildi", color: "#dc2626" },
  request_post_edit: { label: "Düzenleme İstendi", color: "#ca8a04" },
  delete_post: { label: "Gönderi Silindi", color: "#dc2626" },
  block_user: { label: "Kullanıcı Engellendi", color: "#dc2626" },
  unblock_user: { label: "Engel Kaldırıldı", color: "#16a34a" },
  set_user_role: { label: "Rol Değiştirildi", color: "#1d4ed8" },
  moderate_reference: { label: "Referans Moderasyonu", color: "#7c3aed" },
  resolve_reference_dispute: { label: "İtiraz Çözümlendi", color: "#7c3aed" },
  resolve_report: { label: "Şikayet Çözümlendi", color: "#16a34a" },
  dismiss_report: { label: "Şikayet Reddedildi", color: "#6b7280" },
  delete_comment: { label: "Yorum Silindi", color: "#dc2626" },
  add_banned_term: { label: "Yasaklı Kelime Eklendi", color: "#ea580c" },
  remove_banned_term: { label: "Yasaklı Kelime Kaldırıldı", color: "#6b7280" },
};

const TARGET_TYPE_LABELS: Record<string, string> = {
  post: "Gönderi",
  user: "Kullanıcı",
  comment: "Yorum",
  reference: "Referans",
  report: "Şikayet",
  system: "Sistem",
};

const PAGE_SIZE = 25;

export default function AuditLogsPage() {
  const { user } = useAdminAuth();
  const [actionFilter, setActionFilter] = useState("all");
  const [targetTypeFilter, setTargetTypeFilter] = useState("all");
  const [lastDoc, setLastDoc] = useState<QueryDocumentSnapshot<DocumentData> | null>(null);
  const [allLogs, setAllLogs] = useState<AuditLog[]>([]);
  const [expandedId, setExpandedId] = useState<string | null>(null);

  const { isLoading, isFetching } = useQuery({
    queryKey: ["audit-logs", actionFilter, targetTypeFilter],
    queryFn: async () => {
      const constraints: QueryConstraint[] = [
        orderBy("createdAt", "desc"),
        limit(PAGE_SIZE),
      ];

      if (actionFilter !== "all") {
        constraints.unshift(where("action", "==", actionFilter));
      }
      if (targetTypeFilter !== "all") {
        constraints.unshift(where("targetType", "==", targetTypeFilter));
      }

      const q = query(collection(db, "auditLogs"), ...constraints);
      const snapshot = await getDocs(q);
      const logs = snapshot.docs.map((d) => ({
        id: d.id,
        ...d.data(),
      })) as AuditLog[];

      setAllLogs(logs);
      setLastDoc(snapshot.docs[snapshot.docs.length - 1] ?? null);
      return logs;
    },
    enabled: !!user && user.role === "admin",
  });

  const loadMore = async () => {
    if (!lastDoc) return;

    const constraints: QueryConstraint[] = [
      orderBy("createdAt", "desc"),
      startAfter(lastDoc),
      limit(PAGE_SIZE),
    ];
    if (actionFilter !== "all") {
      constraints.unshift(where("action", "==", actionFilter));
    }
    if (targetTypeFilter !== "all") {
      constraints.unshift(where("targetType", "==", targetTypeFilter));
    }

    const q = query(collection(db, "auditLogs"), ...constraints);
    const snapshot = await getDocs(q);
    const moreLogs = snapshot.docs.map((d) => ({
      id: d.id,
      ...d.data(),
    })) as AuditLog[];

    setAllLogs((prev) => [...prev, ...moreLogs]);
    setLastDoc(snapshot.docs[snapshot.docs.length - 1] ?? null);
  };

  if (user && user.role !== "admin") {
    return (
      <AdminLayout>
        <div
          style={{
            textAlign: "center",
            padding: 48,
            color: "#dc2626",
          }}
        >
          <div style={{ fontSize: 48, marginBottom: 16 }}>🔒</div>
          <div style={{ fontSize: 18, fontWeight: 600 }}>
            Bu sayfaya erişim yetkiniz yok.
          </div>
          <div style={{ marginTop: 8, color: "#6b7280" }}>
            Denetim kayıtları yalnızca adminler tarafından görüntülenebilir.
          </div>
        </div>
      </AdminLayout>
    );
  }

  return (
    <AdminLayout>
      <div style={{ maxWidth: 1200, margin: "0 auto" }}>
        <div style={{ marginBottom: 24 }}>
          <h1 style={{ margin: 0, fontSize: 24 }}>Denetim Kayıtları</h1>
          <p style={{ margin: "4px 0 0", color: "#6b7280" }}>
            Tüm admin ve moderatör işlemlerinin kaydı
          </p>
        </div>

        <div
          style={{
            display: "flex",
            gap: 12,
            marginBottom: 20,
            flexWrap: "wrap",
          }}
        >
          <select
            value={actionFilter}
            onChange={(e) => {
              setActionFilter(e.target.value);
              setAllLogs([]);
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
            <option value="all">Tüm İşlemler</option>
            {Object.entries(ACTION_LABELS).map(([k, v]) => (
              <option key={k} value={k}>
                {v.label}
              </option>
            ))}
          </select>

          <select
            value={targetTypeFilter}
            onChange={(e) => {
              setTargetTypeFilter(e.target.value);
              setAllLogs([]);
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
            <option value="all">Tüm Hedef Türleri</option>
            {Object.entries(TARGET_TYPE_LABELS).map(([k, v]) => (
              <option key={k} value={k}>
                {v}
              </option>
            ))}
          </select>
        </div>

        {isLoading ? (
          <div style={{ textAlign: "center", padding: 48, color: "#6b7280" }}>
            Yükleniyor...
          </div>
        ) : allLogs.length === 0 ? (
          <div
            style={{
              backgroundColor: "#f9fafb",
              border: "1px solid #e5e7eb",
              borderRadius: 12,
              padding: 48,
              textAlign: "center",
              color: "#6b7280",
            }}
          >
            Kayıt bulunamadı.
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
              {allLogs.map((log, idx) => {
                const actionInfo = ACTION_LABELS[log.action] ?? {
                  label: log.action,
                  color: "#6b7280",
                };
                const isExpanded = expandedId === log.id;

                return (
                  <div
                    key={log.id}
                    style={{
                      borderBottom:
                        idx < allLogs.length - 1
                          ? "1px solid #e5e7eb"
                          : "none",
                    }}
                  >
                    <div
                      onClick={() =>
                        setExpandedId(isExpanded ? null : log.id)
                      }
                      style={{
                        display: "flex",
                        alignItems: "center",
                        padding: "12px 20px",
                        cursor: "pointer",
                        gap: 12,
                        backgroundColor: isExpanded ? "#f9fafb" : "white",
                      }}
                    >
                      <span
                        style={{
                          width: 10,
                          height: 10,
                          borderRadius: "50%",
                          backgroundColor: actionInfo.color,
                          flexShrink: 0,
                        }}
                      />
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div
                          style={{
                            display: "flex",
                            gap: 8,
                            alignItems: "center",
                            marginBottom: 2,
                          }}
                        >
                          <span
                            style={{
                              fontWeight: 600,
                              fontSize: 13,
                              color: actionInfo.color,
                            }}
                          >
                            {actionInfo.label}
                          </span>
                          <span
                            style={{
                              fontSize: 11,
                              backgroundColor: "#f3f4f6",
                              color: "#6b7280",
                              padding: "1px 6px",
                              borderRadius: 10,
                            }}
                          >
                            {TARGET_TYPE_LABELS[log.targetType] ?? log.targetType}
                          </span>
                        </div>
                        <div
                          style={{
                            fontSize: 13,
                            color: "#374151",
                            whiteSpace: "nowrap",
                            overflow: "hidden",
                            textOverflow: "ellipsis",
                          }}
                        >
                          {log.description}
                        </div>
                      </div>
                      <div style={{ textAlign: "right", flexShrink: 0 }}>
                        <div style={{ fontSize: 12, color: "#6b7280" }}>
                          {formatDistanceToNow(log.createdAt.toDate(), {
                            addSuffix: true,
                            locale: tr,
                          })}
                        </div>
                        <div style={{ fontSize: 11, color: "#9ca3af" }}>
                          {format(log.createdAt.toDate(), "dd.MM.yyyy HH:mm")}
                        </div>
                      </div>
                      <span style={{ color: "#9ca3af", fontSize: 12 }}>
                        {isExpanded ? "▲" : "▼"}
                      </span>
                    </div>

                    {isExpanded && (
                      <div
                        style={{
                          padding: "12px 20px 16px 42px",
                          backgroundColor: "#f9fafb",
                          borderTop: "1px solid #e5e7eb",
                          fontSize: 13,
                        }}
                      >
                        <div style={{ marginBottom: 8 }}>
                          <span style={{ fontWeight: 600 }}>Admin UID: </span>
                          <span
                            style={{
                              fontFamily: "monospace",
                              color: "#6b7280",
                            }}
                          >
                            {log.adminUid}
                          </span>
                        </div>
                        <div style={{ marginBottom: 8 }}>
                          <span style={{ fontWeight: 600 }}>Hedef ID: </span>
                          <span
                            style={{
                              fontFamily: "monospace",
                              color: "#6b7280",
                            }}
                          >
                            {log.targetId}
                          </span>
                        </div>
                        {log.previousState && (
                          <div style={{ marginBottom: 8 }}>
                            <span style={{ fontWeight: 600 }}>Önceki Durum: </span>
                            <code
                              style={{
                                backgroundColor: "#e5e7eb",
                                padding: "2px 6px",
                                borderRadius: 4,
                                fontSize: 12,
                              }}
                            >
                              {JSON.stringify(log.previousState)}
                            </code>
                          </div>
                        )}
                        {log.newState && (
                          <div>
                            <span style={{ fontWeight: 600 }}>Yeni Durum: </span>
                            <code
                              style={{
                                backgroundColor: "#dcfce7",
                                padding: "2px 6px",
                                borderRadius: 4,
                                fontSize: 12,
                              }}
                            >
                              {JSON.stringify(log.newState)}
                            </code>
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>

            {lastDoc && (
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
                    color: "#374151",
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