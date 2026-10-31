"use client";

/**
 * SCR-ADMIN-REF-003: Referans İtiraz Yönetimi Sayfası
 *
 * Yöneticilerin `references` koleksiyonundaki `status == "disputed"` referansları
 * inceleyip itirazı onayla (overturn) veya reddet (uphold) işlemi yapacağı panel.
 *
 * CLAUDE.md §13: Referans sistemi — resolveReferenceDispute CF.
 * docs/16-reference-system-state-machine.md — disputed → approved | rejected
 *
 * NOT: Bu sayfa sadece admin rolüne açıktır (AdminLayout'ta adminOnly).
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

interface DisputedReference {
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
  disputeReason?: string;
  disputedBy?: string;
  disputedAt?: Timestamp;
  isFlagged?: boolean;
  filterRiskLevel?: string;
  createdAt: Timestamp;
}

const RELATIONSHIP_LABELS: Record<string, string> = {
  tenant_to_landlord: "Kiracı → Ev Sahibi",
  landlord_to_tenant: "Ev Sahibi → Kiracı",
  colleague: "İş Arkadaşı",
  neighbor: "Komşu",
  other: "Diğer",
};

const RATING_STARS = (rating: number) => "★".repeat(rating) + "☆".repeat(5 - rating);

interface AppealDetailModalProps {
  reference: DisputedReference;
  onClose: () => void;
  onUphold: (referenceId: string, adminNote: string) => Promise<void>;
  onOverturn: (referenceId: string, adminNote: string) => Promise<void>;
}

function AppealDetailModal({ reference, onClose, onUphold, onOverturn }: AppealDetailModalProps) {
  const [adminNote, setAdminNote] = useState("");
  const [isProcessing, setIsProcessing] = useState(false);
  const [activeAction, setActiveAction] = useState<"uphold" | "overturn" | null>(null);

  const handleUphold = async () => {
    if (!adminNote.trim()) {
      toast.error("Karar gerekçesi zorunludur.");
      return;
    }
    setIsProcessing(true);
    try {
      await onUphold(reference.id, adminNote);
      onClose();
    } finally {
      setIsProcessing(false);
    }
  };

  const handleOverturn = async () => {
    if (!adminNote.trim()) {
      toast.error("Karar gerekçesi zorunludur.");
      return;
    }
    setIsProcessing(true);
    try {
      await onOverturn(reference.id, adminNote);
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
          maxWidth: 700,
          width: "90%",
          maxHeight: "90vh",
          overflowY: "auto",
        }}
      >
        <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 16 }}>
          <h2 style={{ margin: 0, fontSize: 18 }}>Referans İtirazı İnceleme</h2>
          <button
            onClick={onClose}
            style={{ background: "none", border: "none", cursor: "pointer", fontSize: 20 }}
          >
            ✕
          </button>
        </div>

        {/* Uyarı kutusu */}
        <div
          style={{
            backgroundColor: "#fef3c7",
            border: "1px solid #fbbf24",
            borderRadius: 8,
            padding: 12,
            marginBottom: 16,
          }}
        >
          <strong>⚖️ İtiraz İncelemesi</strong> — Bu karar geri alınamaz. Lütfen dikkatli inceleyin.
        </div>

        {/* Orijinal referans */}
        <div
          style={{
            backgroundColor: "#f9fafb",
            border: "1px solid #e5e7eb",
            borderRadius: 8,
            padding: 16,
            marginBottom: 12,
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

        {/* Yanıt (varsa) */}
        {reference.responseText && (
          <div
            style={{
              backgroundColor: "#eff6ff",
              border: "1px solid #bfdbfe",
              borderRadius: 8,
              padding: 16,
              marginBottom: 12,
            }}
          >
            <div style={{ fontSize: 12, color: "#1d4ed8", marginBottom: 8, fontWeight: 600 }}>
              YANIT
            </div>
            <div style={{ lineHeight: 1.6, color: "#374151" }}>{reference.responseText}</div>
          </div>
        )}

        {/* İtiraz detayı */}
        <div
          style={{
            backgroundColor: "#fdf2f8",
            border: "1px solid #f0abfc",
            borderRadius: 8,
            padding: 16,
            marginBottom: 20,
          }}
        >
          <div style={{ fontSize: 12, color: "#7e22ce", marginBottom: 8, fontWeight: 600 }}>
            İTİRAZ
          </div>
          <div style={{ fontSize: 13, marginBottom: 4 }}>
            <strong>İtiraz Eden:</strong>{" "}
            {reference.disputedBy === reference.toUid
              ? `@${reference.toDisplayName} (referans alan)`
              : `@${reference.fromDisplayName} (referans veren)`}
          </div>
          {reference.disputeReason && (
            <div style={{ lineHeight: 1.6, color: "#374151", marginTop: 8 }}>
              <strong>Gerekçe:</strong> {reference.disputeReason}
            </div>
          )}
          {reference.disputedAt && (
            <div style={{ fontSize: 12, color: "#6b7280", marginTop: 8 }}>
              {formatDistanceToNow(reference.disputedAt.toDate(), {
                addSuffix: true,
                locale: tr,
              })}
            </div>
          )}
        </div>

        {/* Karar alanı */}
        <div style={{ marginBottom: 12 }}>
          <label
            style={{ fontSize: 13, color: "#374151", display: "block", marginBottom: 4, fontWeight: 600 }}
          >
            Karar Gerekçesi <span style={{ color: "#dc2626" }}>*</span>
          </label>
          <textarea
            value={adminNote}
            onChange={(e) => setAdminNote(e.target.value)}
            placeholder="Kararınızın gerekçesini yazın (her iki tarafa gösterilecektir)..."
            rows={4}
            style={{
              width: "100%",
              padding: 10,
              borderRadius: 6,
              border: "1px solid #d1d5db",
              resize: "vertical",
              boxSizing: "border-box",
              lineHeight: 1.5,
            }}
          />
        </div>

        {/* Eylemler */}
        <div style={{ display: "flex", gap: 8 }}>
          <button
            onClick={() => {
              setActiveAction("uphold");
              handleUphold();
            }}
            disabled={isProcessing || !adminNote.trim()}
            style={{
              flex: 1,
              padding: "12px 16px",
              backgroundColor:
                isProcessing && activeAction === "uphold" ? "#9ca3af" : "#dc2626",
              color: "white",
              border: "none",
              borderRadius: 8,
              cursor:
                isProcessing || !adminNote.trim() ? "not-allowed" : "pointer",
              fontWeight: 600,
              opacity: !adminNote.trim() ? 0.6 : 1,
            }}
          >
            ✕ İtirazı Reddet
            <div style={{ fontSize: 11, fontWeight: 400, marginTop: 2, opacity: 0.9 }}>
              (Referans onaylı kalır)
            </div>
          </button>
          <button
            onClick={() => {
              setActiveAction("overturn");
              handleOverturn();
            }}
            disabled={isProcessing || !adminNote.trim()}
            style={{
              flex: 1,
              padding: "12px 16px",
              backgroundColor:
                isProcessing && activeAction === "overturn" ? "#9ca3af" : "#16a34a",
              color: "white",
              border: "none",
              borderRadius: 8,
              cursor:
                isProcessing || !adminNote.trim() ? "not-allowed" : "pointer",
              fontWeight: 600,
              opacity: !adminNote.trim() ? 0.6 : 1,
            }}
          >
            ✓ İtirazı Kabul Et
            <div style={{ fontSize: 11, fontWeight: 400, marginTop: 2, opacity: 0.9 }}>
              (Referans kaldırılır)
            </div>
          </button>
        </div>

        {isProcessing && (
          <div style={{ textAlign: "center", marginTop: 12, color: "#6b7280", fontSize: 13 }}>
            İşleniyor...
          </div>
        )}
      </div>
    </div>
  );
}

export default function ReferenceAppealsPage() {
  const { user } = useAdminAuth();
  const [selectedReference, setSelectedReference] = useState<DisputedReference | null>(null);
  const functions = getFunctions(undefined, "europe-west1");

  const {
    data: references = [],
    isLoading,
    refetch,
  } = useQuery({
    queryKey: ["reference-appeals-disputed"],
    queryFn: async () => {
      const q = query(
        collection(db, "references"),
        where("status", "==", "disputed"),
        orderBy("disputedAt", "asc"),
        limit(50)
      );
      const snapshot = await getDocs(q);
      return snapshot.docs.map((doc) => ({
        id: doc.id,
        ...doc.data(),
      })) as DisputedReference[];
    },
    enabled: !!user,
    refetchInterval: 30_000,
  });

  const handleUphold = async (referenceId: string, adminNote: string) => {
    try {
      const resolveReferenceDispute = httpsCallable(functions, "resolveReferenceDispute");
      await resolveReferenceDispute({ referenceId, action: "uphold", adminNote });
      toast.success("İtiraz reddedildi. Referans onaylı kalmaya devam ediyor.");
      refetch();
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : "Bilinmeyen hata";
      toast.error(`Hata: ${message}`);
      throw error;
    }
  };

  const handleOverturn = async (referenceId: string, adminNote: string) => {
    try {
      const resolveReferenceDispute = httpsCallable(functions, "resolveReferenceDispute");
      await resolveReferenceDispute({ referenceId, action: "overturn", adminNote });
      toast.success("İtiraz kabul edildi. Referans kaldırıldı.");
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
            <h1 style={{ margin: 0, fontSize: 24 }}>Referans İtirazları</h1>
            <p style={{ margin: "4px 0 0", color: "#6b7280" }}>
              {references.length} itiraz inceleme bekliyor
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
            <div style={{ fontSize: 18, fontWeight: 600 }}>Bekleyen referans itirazı yok</div>
            <div style={{ marginTop: 8, color: "#6b7280" }}>Tüm itirazlar çözüme kavuşturuldu.</div>
          </div>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
            {references.map((ref) => (
              <div
                key={ref.id}
                style={{
                  backgroundColor: "white",
                  border: "2px solid #f0abfc",
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
                      <span
                        style={{
                          backgroundColor: "#fdf2f8",
                          color: "#7e22ce",
                          padding: "2px 8px",
                          borderRadius: 12,
                          fontSize: 11,
                          fontWeight: 600,
                        }}
                      >
                        İTİRAZ
                      </span>
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
                    <div style={{ color: "#374151", fontSize: 14, marginBottom: 4 }}>
                      {ref.text.substring(0, 120)}
                      {ref.text.length > 120 ? "..." : ""}
                    </div>
                    {ref.disputeReason && (
                      <div
                        style={{
                          backgroundColor: "#fdf2f8",
                          padding: "6px 10px",
                          borderRadius: 6,
                          fontSize: 13,
                          color: "#6b7280",
                        }}
                      >
                        İtiraz: &quot;{ref.disputeReason.substring(0, 80)}
                        {ref.disputeReason.length > 80 ? "..." : ""}&quot;
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
                    <div style={{ color: "#f59e0b" }}>{RATING_STARS(ref.rating)}</div>
                    <span style={{ fontSize: 12, color: "#9ca3af" }}>
                      {ref.disputedAt
                        ? formatDistanceToNow(ref.disputedAt.toDate(), {
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
        <AppealDetailModal
          reference={selectedReference}
          onClose={() => setSelectedReference(null)}
          onUphold={handleUphold}
          onOverturn={handleOverturn}
        />
      )}
    </AdminLayout>
  );
}