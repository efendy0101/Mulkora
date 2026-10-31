"use client";

/**
 * SCR-ADMIN-003: Şikayet Yönetimi Sayfası
 *
 * Moderatörlerin `reports` koleksiyonundaki `status == "new"` şikayetleri
 * inceleyip işlem yapacağı panel sayfası.
 *
 * CLAUDE.md §11: Şikayet sistemi — 5 içerik türü, 13 neden.
 * docs/11-firebase-collections.md — reports koleksiyonu.
 */

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
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

interface Report {
  id: string;
  reporterUid: string;
  reporterUsername: string;
  targetType: "post" | "comment" | "user" | "reference" | "message";
  targetId: string;
  targetOwnerUid?: string;
  targetOwnerUsername?: string;
  reason: string;
  description?: string;
  status: "new" | "reviewed" | "resolved" | "dismissed";
  priority: "low" | "medium" | "high" | "critical";
  createdAt: Timestamp;
}

const REASON_LABELS: Record<string, string> = {
  spam: "Spam",
  hate_speech: "Nefret söylemi",
  harassment: "Taciz",
  violence: "Şiddet",
  misinformation: "Yanlış bilgi",
  sexual_content: "Cinsel içerik",
  illegal_content: "Yasadışı içerik",
  privacy_violation: "Gizlilik ihlali",
  fraud: "Dolandırıcılık",
  off_topic: "Konu dışı",
  duplicate: "Tekrar içerik",
  other: "Diğer",
  impersonation: "Kimlik taklidi",
};

const TARGET_TYPE_LABELS: Record<string, string> = {
  post: "Gönderi",
  comment: "Yorum",
  user: "Kullanıcı",
  reference: "Referans",
  message: "Mesaj",
};

const PRIORITY_COLORS: Record<string, string> = {
  critical: "#dc2626",
  high: "#ea580c",
  medium: "#ca8a04",
  low: "#16a34a",
};

const STATUS_LABELS: Record<string, string> = {
  new: "Yeni",
  reviewed: "İncelendi",
  resolved: "Çözüldü",
  dismissed: "Reddedildi",
};

interface ReportDetailModalProps {
  report: Report;
  onClose: () => void;
  onResolve: (reportId: string, resolution: string, action: string) => Promise<void>;
  onDismiss: (reportId: string, reason: string) => Promise<void>;
}

function ReportDetailModal({ report, onClose, onResolve, onDismiss }: ReportDetailModalProps) {
  const [resolution, setResolution] = useState("");
  const [dismissReason, setDismissReason] = useState("");
  const [selectedAction, setSelectedAction] = useState("warn");
  const [isProcessing, setIsProcessing] = useState(false);
  const [activeAction, setActiveAction] = useState<"resolve" | "dismiss" | null>(null);

  const handleResolve = async () => {
    if (!resolution.trim()) {
      toast.error("Çözüm açıklaması zorunludur.");
      return;
    }
    setIsProcessing(true);
    try {
      await onResolve(report.id, resolution, selectedAction);
      onClose();
    } finally {
      setIsProcessing(false);
    }
  };

  const handleDismiss = async () => {
    if (!dismissReason.trim()) {
      toast.error("Reddetme nedeni zorunludur.");
      return;
    }
    setIsProcessing(true);
    try {
      await onDismiss(report.id, dismissReason);
      onClose();
    } finally {
      setIsProcessing(false);
    }
  };

  return (
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
          maxWidth: 600,
          width: "90%",
          maxHeight: "90vh",
          overflowY: "auto",
        }}
      >
        <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 16 }}>
          <h2 style={{ margin: 0, fontSize: 18 }}>Şikayet İnceleme</h2>
          <button
            onClick={onClose}
            style={{ background: "none", border: "none", cursor: "pointer", fontSize: 20 }}
          >
            ✕
          </button>
        </div>

        <div
          style={{
            backgroundColor: "#fef9c3",
            border: "1px solid #fde047",
            borderRadius: 8,
            padding: 12,
            marginBottom: 16,
          }}
        >
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <span style={{ fontWeight: 600, fontSize: 14 }}>
              {TARGET_TYPE_LABELS[report.targetType]} Şikayeti
            </span>
            <span
              style={{
                backgroundColor: PRIORITY_COLORS[report.priority],
                color: "white",
                padding: "2px 8px",
                borderRadius: 12,
                fontSize: 12,
                fontWeight: 600,
              }}
            >
              {report.priority.toUpperCase()}
            </span>
          </div>
        </div>

        <div style={{ marginBottom: 12 }}>
          <div style={{ fontSize: 13, color: "#6b7280" }}>Şikayet Eden</div>
          <div style={{ fontWeight: 600 }}>@{report.reporterUsername}</div>
        </div>

        {report.targetOwnerUsername && (
          <div style={{ marginBottom: 12 }}>
            <div style={{ fontSize: 13, color: "#6b7280" }}>Şikayet Edilen</div>
            <div style={{ fontWeight: 600 }}>@{report.targetOwnerUsername}</div>
          </div>
        )}

        <div style={{ marginBottom: 12 }}>
          <div style={{ fontSize: 13, color: "#6b7280" }}>Neden</div>
          <div style={{ fontWeight: 600 }}>{REASON_LABELS[report.reason] ?? report.reason}</div>
        </div>

        <div style={{ marginBottom: 12 }}>
          <div style={{ fontSize: 13, color: "#6b7280" }}>Hedef ID</div>
          <div style={{ fontSize: 12, fontFamily: "monospace", color: "#6b7280" }}>
            {report.targetId}
          </div>
        </div>

        {report.description && (
          <div style={{ marginBottom: 16 }}>
            <div style={{ fontSize: 13, color: "#6b7280", marginBottom: 4 }}>Açıklama</div>
            <div
              style={{
                backgroundColor: "#f9fafb",
                border: "1px solid #e5e7eb",
                borderRadius: 8,
                padding: 12,
                lineHeight: 1.6,
              }}
            >
              {report.description}
            </div>
          </div>
        )}

        <div style={{ marginBottom: 20 }}>
          <div style={{ fontSize: 13, color: "#6b7280" }}>
            {formatDistanceToNow(report.createdAt.toDate(), {
              addSuffix: true,
              locale: tr,
            })}
          </div>
        </div>

        {/* Eylemler */}
        <div style={{ display: "flex", gap: 8, marginBottom: activeAction ? 12 : 0 }}>
          <button
            onClick={() => setActiveAction(activeAction === "resolve" ? null : "resolve")}
            style={{
              flex: 1,
              padding: "10px 16px",
              backgroundColor: activeAction === "resolve" ? "#1d4ed8" : "#eff6ff",
              color: activeAction === "resolve" ? "white" : "#1d4ed8",
              border: "1px solid #1d4ed8",
              borderRadius: 8,
              cursor: "pointer",
              fontWeight: 600,
            }}
          >
            ✓ İşlem Al
          </button>
          <button
            onClick={() => setActiveAction(activeAction === "dismiss" ? null : "dismiss")}
            style={{
              flex: 1,
              padding: "10px 16px",
              backgroundColor: activeAction === "dismiss" ? "#6b7280" : "#f3f4f6",
              color: activeAction === "dismiss" ? "white" : "#6b7280",
              border: "1px solid #6b7280",
              borderRadius: 8,
              cursor: "pointer",
              fontWeight: 600,
            }}
          >
            ✕ Reddet
          </button>
        </div>

        {activeAction === "resolve" && (
          <div style={{ marginTop: 12 }}>
            <label
              style={{ fontSize: 13, color: "#374151", display: "block", marginBottom: 4 }}
            >
              Yapılan İşlem
            </label>
            <select
              value={selectedAction}
              onChange={(e) => setSelectedAction(e.target.value)}
              style={{
                width: "100%",
                padding: 8,
                borderRadius: 6,
                border: "1px solid #d1d5db",
                marginBottom: 8,
              }}
            >
              <option value="warn">Kullanıcıyı Uyar</option>
              <option value="remove_content">İçeriği Kaldır</option>
              <option value="ban_temporary">Geçici Yasak (7 gün)</option>
              <option value="ban_permanent">Kalıcı Yasak</option>
              <option value="no_action">İşlem Yok</option>
            </select>
            <label
              style={{ fontSize: 13, color: "#374151", display: "block", marginBottom: 4 }}
            >
              Çözüm Açıklaması <span style={{ color: "#dc2626" }}>*</span>
            </label>
            <textarea
              value={resolution}
              onChange={(e) => setResolution(e.target.value)}
              placeholder="Alınan kararı açıklayın..."
              rows={3}
              style={{
                width: "100%",
                padding: 8,
                borderRadius: 6,
                border: "1px solid #d1d5db",
                resize: "vertical",
                boxSizing: "border-box",
              }}
            />
            <button
              onClick={handleResolve}
              disabled={isProcessing}
              style={{
                marginTop: 8,
                width: "100%",
                padding: "10px 16px",
                backgroundColor: "#1d4ed8",
                color: "white",
                border: "none",
                borderRadius: 8,
                cursor: isProcessing ? "not-allowed" : "pointer",
                fontWeight: 600,
              }}
            >
              {isProcessing ? "İşleniyor..." : "İşlemi Onayla"}
            </button>
          </div>
        )}

        {activeAction === "dismiss" && (
          <div style={{ marginTop: 12 }}>
            <label
              style={{ fontSize: 13, color: "#374151", display: "block", marginBottom: 4 }}
            >
              Reddetme Nedeni <span style={{ color: "#dc2626" }}>*</span>
            </label>
            <textarea
              value={dismissReason}
              onChange={(e) => setDismissReason(e.target.value)}
              placeholder="Şikayeti neden reddediyorsunuz?"
              rows={3}
              style={{
                width: "100%",
                padding: 8,
                borderRadius: 6,
                border: "1px solid #d1d5db",
                resize: "vertical",
                boxSizing: "border-box",
              }}
            />
            <button
              onClick={handleDismiss}
              disabled={isProcessing}
              style={{
                marginTop: 8,
                width: "100%",
                padding: "10px 16px",
                backgroundColor: "#6b7280",
                color: "white",
                border: "none",
                borderRadius: 8,
                cursor: isProcessing ? "not-allowed" : "pointer",
                fontWeight: 600,
              }}
            >
              {isProcessing ? "İşleniyor..." : "Reddi Onayla"}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

export default function ModerationReportsPage() {
  const { user } = useAdminAuth();
  const [selectedReport, setSelectedReport] = useState<Report | null>(null);
  const [statusFilter, setStatusFilter] = useState<"new" | "all">("new");

  const {
    data: reports = [],
    isLoading,
    refetch,
  } = useQuery({
    queryKey: ["moderation-reports", statusFilter],
    queryFn: async () => {
      const constraints =
        statusFilter === "new"
          ? [where("status", "==", "new"), orderBy("createdAt", "asc"), limit(50)]
          : [orderBy("createdAt", "desc"), limit(50)];

      const q = query(collection(db, "reports"), ...constraints);
      const snapshot = await getDocs(q);
      return snapshot.docs.map((doc) => ({
        id: doc.id,
        ...doc.data(),
      })) as Report[];
    },
    enabled: !!user,
    refetchInterval: 30_000,
  });

  const handleResolve = async (reportId: string, resolution: string, action: string) => {
    try {
      const resolveReport = httpsCallable(functions, "resolveReport");
      await resolveReport({ reportId, resolution, action });
      toast.success("Şikayet çözüldü.");
      refetch();
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : "Bilinmeyen hata";
      toast.error(`Hata: ${message}`);
      throw error;
    }
  };

  const handleDismiss = async (reportId: string, reason: string) => {
    try {
      const dismissReport = httpsCallable(functions, "dismissReport");
      await dismissReport({ reportId, reason });
      toast.success("Şikayet reddedildi.");
      refetch();
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : "Bilinmeyen hata";
      toast.error(`Hata: ${message}`);
      throw error;
    }
  };

  return (
    <AdminLayout>
      <div style={{ maxWidth: 960, margin: "0 auto" }}>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            marginBottom: 24,
          }}
        >
          <div>
            <h1 style={{ margin: 0, fontSize: 24 }}>Şikayet Yönetimi</h1>
            <p style={{ margin: "4px 0 0", color: "#6b7280" }}>
              {reports.length} şikayet listeleniyor
            </p>
          </div>
          <div style={{ display: "flex", gap: 8 }}>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as "new" | "all")}
              style={{
                padding: "8px 12px",
                border: "1px solid #d1d5db",
                borderRadius: 8,
                background: "white",
              }}
            >
              <option value="new">Yeni Şikayetler</option>
              <option value="all">Tüm Şikayetler</option>
            </select>
            <button
              onClick={() => refetch()}
              style={{
                padding: "8px 16px",
                border: "1px solid #d1d5db",
                borderRadius: 8,
                background: "white",
                cursor: "pointer",
              }}
            >
              ↻ Yenile
            </button>
          </div>
        </div>

        {isLoading ? (
          <div style={{ textAlign: "center", padding: 48, color: "#6b7280" }}>
            Yükleniyor...
          </div>
        ) : reports.length === 0 ? (
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
            <div style={{ fontSize: 18, fontWeight: 600 }}>Bekleyen şikayet yok</div>
            <div style={{ marginTop: 8, color: "#6b7280" }}>Tüm şikayetler incelendi.</div>
          </div>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
            {reports.map((report) => (
              <div
                key={report.id}
                style={{
                  backgroundColor: "white",
                  border:
                    report.priority === "critical" || report.priority === "high"
                      ? "2px solid #fca5a5"
                      : "1px solid #e5e7eb",
                  borderRadius: 12,
                  padding: 20,
                  cursor: "pointer",
                }}
                onClick={() => setSelectedReport(report)}
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "flex-start",
                  }}
                >
                  <div style={{ flex: 1 }}>
                    <div
                      style={{
                        display: "flex",
                        gap: 6,
                        marginBottom: 8,
                        alignItems: "center",
                      }}
                    >
                      <span
                        style={{
                          backgroundColor: "#f3f4f6",
                          color: "#374151",
                          padding: "2px 8px",
                          borderRadius: 12,
                          fontSize: 11,
                          fontWeight: 600,
                        }}
                      >
                        {TARGET_TYPE_LABELS[report.targetType]}
                      </span>
                      <span
                        style={{
                          backgroundColor: "#fef9c3",
                          color: "#854d0e",
                          padding: "2px 8px",
                          borderRadius: 12,
                          fontSize: 11,
                        }}
                      >
                        {REASON_LABELS[report.reason] ?? report.reason}
                      </span>
                      <span
                        style={{
                          backgroundColor: "#f3f4f6",
                          color: "#374151",
                          padding: "2px 8px",
                          borderRadius: 12,
                          fontSize: 11,
                        }}
                      >
                        {STATUS_LABELS[report.status]}
                      </span>
                    </div>
                    <div style={{ fontSize: 14, color: "#374151" }}>
                      <strong>@{report.reporterUsername}</strong> şikayet etti
                      {report.targetOwnerUsername
                        ? ` → @${report.targetOwnerUsername}`
                        : ""}
                    </div>
                    {report.description && (
                      <div
                        style={{
                          color: "#6b7280",
                          fontSize: 13,
                          marginTop: 4,
                          overflow: "hidden",
                          display: "-webkit-box",
                          WebkitLineClamp: 1,
                          WebkitBoxOrient: "vertical",
                        }}
                      >
                        {report.description}
                      </div>
                    )}
                  </div>
                  <div
                    style={{
                      display: "flex",
                      flexDirection: "column",
                      alignItems: "flex-end",
                      gap: 4,
                      marginLeft: 16,
                    }}
                  >
                    <span
                      style={{
                        backgroundColor: PRIORITY_COLORS[report.priority],
                        color: "white",
                        padding: "2px 8px",
                        borderRadius: 12,
                        fontSize: 11,
                        fontWeight: 600,
                      }}
                    >
                      {report.priority.toUpperCase()}
                    </span>
                    <span style={{ fontSize: 12, color: "#9ca3af" }}>
                      {formatDistanceToNow(report.createdAt.toDate(), {
                        addSuffix: true,
                        locale: tr,
                      })}
                    </span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {selectedReport && (
        <ReportDetailModal
          report={selectedReport}
          onClose={() => setSelectedReport(null)}
          onResolve={handleResolve}
          onDismiss={handleDismiss}
        />
      )}
    </AdminLayout>
  );
}