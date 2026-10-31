"use client";

/**
 * SCR-ADMIN-REF-002: Referans Yanıtları Moderasyon Sayfası
 *
 * Moderatörlerin `references` koleksiyonundaki `status == "response_pending_review"`
 * kayıtları inceleyip onayla / reddet işlemi yapacağı panel sayfası.
 *
 * CLAUDE.md §13: Referans sistemi — moderateReferenceResponse CF.
 * docs/16-reference-system-state-machine.md — response_pending_review → approved | rejected
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
import { getFunctions, httpsCallable } from "firebase/functions";
import { db } from "@/lib/firebase";
import { useAdminAuth } from "@/hooks/useAdminAuth";
import { AdminLayout } from "@/components/AdminLayout";
import toast from "react-hot-toast";
import { formatDistanceToNow } from "date-fns";
import { tr } from "date-fns/locale";

interface ReferenceWithResponse {
  id: string;
  fromUid: string;
  fromDisplayName: string;
  toUid: string;
  toDisplayName: string;
  status: string;
  relationship: string;
  rating: number;
  text: string;
  responseText?: string;
  responseRating?: number;
  isFlagged?: boolean;
  flagReason?: string;
  filterRiskLevel?: string;
  createdAt: Timestamp;
  respondedAt?: Timestamp;
}

const RELATIONSHIP_LABELS: Record<string, string> = {
  tenant_to_landlord: "Kiracı → Ev Sahibi",
  landlord_to_tenant: "Ev Sahibi → Kiracı",
  colleague: "İş Arkadaşı",
  neighbor: "Komşu",
  other: "Diğer",
};

const RATING_STARS = (rating: number) => "★".repeat(rating) + "☆".repeat(5 - rating);

interface ResponseDetailModalProps {
  reference: ReferenceWithResponse;
  onClose: () => void;
  onApprove: (referenceId: string, moderatorNote: string) => Promise<void>;
  onReject: (referenceId: string, reason: string) => Promise<void>;
}

function ResponseDetailModal({ reference, onClose, onApprove, onReject }: ResponseDetailModalProps) {
  const [moderatorNote, setModeratorNote] = useState("");
  const [rejectReason, setRejectReason] = useState("");
  const [isProcessing, setIsProcessing] = useState(false);
  const [activeAction, setActiveAction] = useState<"approve" | "reject" | null>(null);

  const handleApprove = async () => {
    setIsProcessing(true);
    try {
      await onApprove(reference.id, moderatorNote);
      onClose();
    } finally {
      setIsProcessing(false);
    }
  };

  const handleReject = async () => {
    if (!rejectReason.trim()) {
      toast.error("Reddetme nedeni zorunludur.");
      return;
    }
    setIsProcessing(true);
    try {
      await onReject(reference.id, rejectReason);
      onClose();
    } finally {
      setIsProcessing(false);
    }
  };

  const riskColor =
    reference.filterRiskLevel === "CRITICAL"
      ? "#dc2626"
      : reference.filterRiskLevel === "HIGH"
      ? "#ea580c"
      : reference.filterRiskLevel === "MEDIUM"
      ? "#ca8a04"
      : "#16a34a";

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
          maxWidth: 680,
          width: "90%",
          maxHeight: "90vh",
          overflowY: "auto",
        }}
      >
        <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 16 }}>
          <h2 style={{ margin: 0, fontSize: 18 }}>Referans Yanıtı İnceleme</h2>
          <button
            onClick={onClose}
            style={{ background: "none", border: "none", cursor: "pointer", fontSize: 20 }}
          >
            ✕
          </button>
        </div>

        {reference.isFlagged && (
          <div
            style={{
              backgroundColor: "#fef2f2",
              border: "1px solid #fca5a5",
              borderRadius: 8,
              padding: 12,
              marginBottom: 16,
              color: "#991b1b",
            }}
          >
            ⚠️ İçerik Filtresi Uyarısı:{" "}
            <strong style={{ color: riskColor }}>{reference.filterRiskLevel}</strong>
            {reference.flagReason && (
              <div style={{ marginTop: 4, fontSize: 13 }}>{reference.flagReason}</div>
            )}
          </div>
        )}

        {/* Referans bilgisi */}
        <div
          style={{
            backgroundColor: "#f9fafb",
            border: "1px solid #e5e7eb",
            borderRadius: 8,
            padding: 16,
            marginBottom: 16,
          }}
        >
          <div style={{ fontSize: 12, color: "#6b7280", marginBottom: 8, fontWeight: 600 }}>
            ORİJİNAL REFERANS
          </div>
          <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 8 }}>
            <div>
              <span style={{ fontWeight: 600 }}>@{reference.fromDisplayName}</span>
              <span style={{ color: "#6b7280", margin: "0 8px" }}>→</span>
              <span style={{ fontWeight: 600 }}>@{reference.toDisplayName}</span>
            </div>
            <div style={{ color: "#f59e0b", fontSize: 16 }}>
              {RATING_STARS(reference.rating)}
            </div>
          </div>
          <div style={{ fontSize: 12, color: "#6b7280", marginBottom: 4 }}>
            {RELATIONSHIP_LABELS[reference.relationship] ?? reference.relationship}
          </div>
          <div style={{ lineHeight: 1.6, color: "#374151" }}>{reference.text}</div>
        </div>

        {/* Yanıt */}
        <div
          style={{
            backgroundColor: "#eff6ff",
            border: "1px solid #bfdbfe",
            borderRadius: 8,
            padding: 16,
            marginBottom: 16,
          }}
        >
          <div style={{ fontSize: 12, color: "#1d4ed8", marginBottom: 8, fontWeight: 600 }}>
            YANIT (İNCELENECEK)
          </div>
          {reference.responseRating !== undefined && (
            <div style={{ color: "#f59e0b", fontSize: 16, marginBottom: 8 }}>
              {RATING_STARS(reference.responseRating)}
            </div>
          )}
          <div style={{ lineHeight: 1.6, color: "#374151" }}>{reference.responseText}</div>
          {reference.respondedAt && (
            <div style={{ fontSize: 12, color: "#6b7280", marginTop: 8 }}>
              {formatDistanceToNow(reference.respondedAt.toDate(), {
                addSuffix: true,
                locale: tr,
              })}
            </div>
          )}
        </div>

        {/* Eylemler */}
        <div style={{ display: "flex", gap: 8, marginBottom: activeAction ? 12 : 0 }}>
          <button
            onClick={() => setActiveAction(activeAction === "approve" ? null : "approve")}
            style={{
              flex: 1,
              padding: "10px 16px",
              backgroundColor: activeAction === "approve" ? "#16a34a" : "#dcfce7",
              color: activeAction === "approve" ? "white" : "#16a34a",
              border: "1px solid #16a34a",
              borderRadius: 8,
              cursor: "pointer",
              fontWeight: 600,
            }}
          >
            ✓ Yanıtı Onayla
          </button>
          <button
            onClick={() => setActiveAction(activeAction === "reject" ? null : "reject")}
            style={{
              flex: 1,
              padding: "10px 16px",
              backgroundColor: activeAction === "reject" ? "#dc2626" : "#fee2e2",
              color: activeAction === "reject" ? "white" : "#dc2626",
              border: "1px solid #dc2626",
              borderRadius: 8,
              cursor: "pointer",
              fontWeight: 600,
            }}
          >
            ✕ Yanıtı Reddet
          </button>
        </div>

        {activeAction === "approve" && (
          <div style={{ marginTop: 12 }}>
            <label
              style={{ fontSize: 13, color: "#374151", display: "block", marginBottom: 4 }}
            >
              Moderatör Notu (opsiyonel)
            </label>
            <textarea
              value={moderatorNote}
              onChange={(e) => setModeratorNote(e.target.value)}
              placeholder="Onay notu ekleyin..."
              rows={2}
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
              onClick={handleApprove}
              disabled={isProcessing}
              style={{
                marginTop: 8,
                width: "100%",
                padding: "10px 16px",
                backgroundColor: "#16a34a",
                color: "white",
                border: "none",
                borderRadius: 8,
                cursor: isProcessing ? "not-allowed" : "pointer",
                fontWeight: 600,
              }}
            >
              {isProcessing ? "İşleniyor..." : "Onayı Onayla"}
            </button>
          </div>
        )}

        {activeAction === "reject" && (
          <div style={{ marginTop: 12 }}>
            <label
              style={{ fontSize: 13, color: "#374151", display: "block", marginBottom: 4 }}
            >
              Reddetme Nedeni <span style={{ color: "#dc2626" }}>*</span>
            </label>
            <textarea
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              placeholder="Yanıtı neden reddettiğinizi açıklayın..."
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
              onClick={handleReject}
              disabled={isProcessing}
              style={{
                marginTop: 8,
                width: "100%",
                padding: "10px 16px",
                backgroundColor: "#dc2626",
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

export default function ReferenceResponsesPage() {
  const { user } = useAdminAuth();
  const [selectedReference, setSelectedReference] = useState<ReferenceWithResponse | null>(null);
  const functions = getFunctions(undefined, "europe-west1");

  const {
    data: references = [],
    isLoading,
    refetch,
  } = useQuery({
    queryKey: ["reference-responses-pending"],
    queryFn: async () => {
      const q = query(
        collection(db, "references"),
        where("status", "==", "response_pending_review"),
        orderBy("respondedAt", "asc"),
        limit(50)
      );
      const snapshot = await getDocs(q);
      return snapshot.docs.map((doc) => ({
        id: doc.id,
        ...doc.data(),
      })) as ReferenceWithResponse[];
    },
    enabled: !!user,
    refetchInterval: 30_000,
  });

  const handleApprove = async (referenceId: string, moderatorNote: string) => {
    try {
      const moderateReferenceResponse = httpsCallable(functions, "moderateReferenceResponse");
      await moderateReferenceResponse({ referenceId, action: "approve", moderatorNote });
      toast.success("Referans yanıtı onaylandı.");
      refetch();
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : "Bilinmeyen hata";
      toast.error(`Hata: ${message}`);
      throw error;
    }
  };

  const handleReject = async (referenceId: string, reason: string) => {
    try {
      const moderateReferenceResponse = httpsCallable(functions, "moderateReferenceResponse");
      await moderateReferenceResponse({ referenceId, action: "reject", reason });
      toast.success("Referans yanıtı reddedildi.");
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
            <h1 style={{ margin: 0, fontSize: 24 }}>Referans Yanıtları</h1>
            <p style={{ margin: "4px 0 0", color: "#6b7280" }}>
              {references.length} yanıt inceleme bekliyor
            </p>
          </div>
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

        {isLoading ? (
          <div style={{ textAlign: "center", padding: 48, color: "#6b7280" }}>
            Yükleniyor...
          </div>
        ) : references.length === 0 ? (
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
            <div style={{ fontSize: 18, fontWeight: 600 }}>Bekleyen referans yanıtı yok</div>
            <div style={{ marginTop: 8, color: "#6b7280" }}>Tüm yanıtlar incelendi.</div>
          </div>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
            {references.map((ref) => (
              <div
                key={ref.id}
                style={{
                  backgroundColor: "white",
                  border: ref.isFlagged ? "2px solid #fca5a5" : "1px solid #e5e7eb",
                  borderRadius: 12,
                  padding: 20,
                  cursor: "pointer",
                }}
                onClick={() => setSelectedReference(ref)}
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "flex-start",
                  }}
                >
                  <div style={{ flex: 1 }}>
                    <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 6 }}>
                      <span style={{ fontWeight: 600 }}>@{ref.fromDisplayName}</span>
                      <span style={{ color: "#6b7280" }}>→</span>
                      <span style={{ fontWeight: 600 }}>@{ref.toDisplayName}</span>
                      <span
                        style={{
                          backgroundColor: "#eff6ff",
                          color: "#1d4ed8",
                          padding: "2px 8px",
                          borderRadius: 12,
                          fontSize: 11,
                        }}
                      >
                        {RELATIONSHIP_LABELS[ref.relationship] ?? ref.relationship}
                      </span>
                    </div>
                    <div
                      style={{
                        color: "#6b7280",
                        fontSize: 13,
                        marginBottom: 4,
                        fontStyle: "italic",
                      }}
                    >
                      Orijinal: &quot;{ref.text.substring(0, 80)}
                      {ref.text.length > 80 ? "..." : ""}&quot;
                    </div>
                    <div
                      style={{
                        color: "#374151",
                        fontSize: 14,
                        backgroundColor: "#eff6ff",
                        padding: "6px 10px",
                        borderRadius: 6,
                      }}
                    >
                      Yanıt: &quot;
                      {ref.responseText
                        ? ref.responseText.substring(0, 100) +
                          (ref.responseText.length > 100 ? "..." : "")
                        : "-"}&quot;
                    </div>
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
                    {ref.isFlagged && (
                      <span
                        style={{
                          backgroundColor: "#fef2f2",
                          color: "#991b1b",
                          padding: "2px 8px",
                          borderRadius: 12,
                          fontSize: 12,
                          fontWeight: 600,
                        }}
                      >
                        {ref.filterRiskLevel} RİSK
                      </span>
                    )}
                    <span style={{ fontSize: 12, color: "#9ca3af" }}>
                      {ref.respondedAt
                        ? formatDistanceToNow(ref.respondedAt.toDate(), {
                            addSuffix: true,
                            locale: tr,
                          })
                        : ""}
                    </span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {selectedReference && (
        <ResponseDetailModal
          reference={selectedReference}
          onClose={() => setSelectedReference(null)}
          onApprove={handleApprove}
          onReject={handleReject}
        />
      )}
    </AdminLayout>
  );
}