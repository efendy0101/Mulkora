"use client";

/**
 * SCR-ADMIN-005: Yorum Moderasyon Sayfası
 *
 * Moderatörlerin inceleme kuyruğundaki yorumları
 * (status == "pending_review") inceleyip işlem yapacağı sayfa.
 *
 * CLAUDE.md §9: Yüksek riskli yorumlar doğrudan yayımlanmamalı,
 * moderasyon kuyruğuna gönderilmelidir.
 * docs/11-firebase-collections.md — comments koleksiyonu.
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
  updateDoc,
  doc,
  Timestamp,
} from "firebase/firestore";
import { httpsCallable } from "firebase/functions";
import { db, functions } from "@/lib/firebase";
import { useAdminAuth } from "@/hooks/useAdminAuth";
import { AdminLayout } from "@/components/AdminLayout";
import toast from "react-hot-toast";
import { formatDistanceToNow } from "date-fns";
import { tr } from "date-fns/locale";

interface PendingComment {
  id: string;
  postId: string;
  authorUid: string;
  authorUsername: string;
  content: string;
  status: "pending_review" | "approved" | "rejected";
  riskLevel?: "low" | "medium" | "high" | "critical";
  parentCommentId?: string;
  createdAt: Timestamp;
}

const RISK_COLORS: Record<string, string> = {
  critical: "#dc2626",
  high: "#ea580c",
  medium: "#ca8a04",
  low: "#16a34a",
};

export default function ModerationCommentsPage() {
  const { user } = useAdminAuth();
  const [processingId, setProcessingId] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<"pending_review" | "all">(
    "pending_review"
  );

  const {
    data: comments = [],
    isLoading,
    refetch,
  } = useQuery({
    queryKey: ["moderation-comments", statusFilter],
    queryFn: async () => {
      const constraints =
        statusFilter === "pending_review"
          ? [
              where("status", "==", "pending_review"),
              orderBy("createdAt", "asc"),
              limit(50),
            ]
          : [orderBy("createdAt", "desc"), limit(50)];

      const q = query(collection(db, "comments"), ...constraints);
      const snapshot = await getDocs(q);
      return snapshot.docs.map((d) => ({
        id: d.id,
        ...d.data(),
      })) as PendingComment[];
    },
    enabled: !!user,
    refetchInterval: 30_000,
  });

  const handleApprove = async (comment: PendingComment) => {
    setProcessingId(comment.id);
    try {
      await updateDoc(doc(db, "comments", comment.id), {
        status: "approved",
        reviewedAt: new Date(),
        reviewedBy: user?.uid,
      });
      toast.success("Yorum onaylandı.");
      refetch();
    } catch (error: unknown) {
      toast.error(
        `Hata: ${error instanceof Error ? error.message : "Bilinmeyen hata"}`
      );
    } finally {
      setProcessingId(null);
    }
  };

  const handleDelete = async (comment: PendingComment) => {
    if (!confirm("Bu yorumu silmek istediğinizden emin misiniz?")) return;
    setProcessingId(comment.id);
    try {
      const deleteCommentFn = httpsCallable(functions, "deleteComment");
      await deleteCommentFn({ commentId: comment.id, reason: "moderation" });
      toast.success("Yorum silindi.");
      refetch();
    } catch (error: unknown) {
      toast.error(
        `Hata: ${error instanceof Error ? error.message : "Bilinmeyen hata"}`
      );
    } finally {
      setProcessingId(null);
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
            <h1 style={{ margin: 0, fontSize: 24 }}>Yorum Yönetimi</h1>
            <p style={{ margin: "4px 0 0", color: "#6b7280" }}>
              {comments.length} yorum listeleniyor
            </p>
          </div>
          <div style={{ display: "flex", gap: 8 }}>
            <select
              value={statusFilter}
              onChange={(e) =>
                setStatusFilter(e.target.value as "pending_review" | "all")
              }
              style={{
                padding: "8px 12px",
                border: "1px solid #d1d5db",
                borderRadius: 8,
                background: "white",
              }}
            >
              <option value="pending_review">İnceleme Bekleyenler</option>
              <option value="all">Tüm Yorumlar</option>
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
        ) : comments.length === 0 ? (
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
              İnceleme bekleyen yorum yok
            </div>
            <div style={{ marginTop: 8, color: "#6b7280" }}>
              Tüm yorumlar işlendi.
            </div>
          </div>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
            {comments.map((comment) => (
              <div
                key={comment.id}
                style={{
                  backgroundColor: "white",
                  border:
                    comment.riskLevel === "high" ||
                    comment.riskLevel === "critical"
                      ? "2px solid #fca5a5"
                      : "1px solid #e5e7eb",
                  borderRadius: 12,
                  padding: 20,
                }}
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "flex-start",
                    marginBottom: 12,
                  }}
                >
                  <div
                    style={{
                      display: "flex",
                      gap: 8,
                      alignItems: "center",
                      flexWrap: "wrap",
                    }}
                  >
                    <span style={{ fontWeight: 600 }}>
                      @{comment.authorUsername}
                    </span>
                    {comment.parentCommentId && (
                      <span
                        style={{
                          backgroundColor: "#e0f2fe",
                          color: "#0369a1",
                          padding: "2px 8px",
                          borderRadius: 12,
                          fontSize: 11,
                        }}
                      >
                        Cevap
                      </span>
                    )}
                    {comment.riskLevel && (
                      <span
                        style={{
                          backgroundColor: RISK_COLORS[comment.riskLevel],
                          color: "white",
                          padding: "2px 8px",
                          borderRadius: 12,
                          fontSize: 11,
                          fontWeight: 600,
                        }}
                      >
                        {comment.riskLevel.toUpperCase()}
                      </span>
                    )}
                  </div>
                  <span style={{ fontSize: 12, color: "#9ca3af" }}>
                    {formatDistanceToNow(comment.createdAt.toDate(), {
                      addSuffix: true,
                      locale: tr,
                    })}
                  </span>
                </div>

                <div
                  style={{
                    backgroundColor: "#f9fafb",
                    border: "1px solid #e5e7eb",
                    borderRadius: 8,
                    padding: 12,
                    marginBottom: 12,
                    lineHeight: 1.6,
                  }}
                >
                  {comment.content}
                </div>

                <div
                  style={{
                    fontSize: 12,
                    color: "#6b7280",
                    marginBottom: 12,
                  }}
                >
                  Gönderi:{" "}
                  <span style={{ fontFamily: "monospace" }}>
                    {comment.postId}
                  </span>
                </div>

                <div style={{ display: "flex", gap: 8 }}>
                  <button
                    onClick={() => handleApprove(comment)}
                    disabled={processingId === comment.id}
                    style={{
                      padding: "8px 16px",
                      backgroundColor: "#16a34a",
                      color: "white",
                      border: "none",
                      borderRadius: 8,
                      cursor:
                        processingId === comment.id
                          ? "not-allowed"
                          : "pointer",
                      fontWeight: 600,
                      opacity: processingId === comment.id ? 0.6 : 1,
                    }}
                  >
                    ✓ Onayla
                  </button>
                  <button
                    onClick={() => handleDelete(comment)}
                    disabled={processingId === comment.id}
                    style={{
                      padding: "8px 16px",
                      backgroundColor: "#dc2626",
                      color: "white",
                      border: "none",
                      borderRadius: 8,
                      cursor:
                        processingId === comment.id
                          ? "not-allowed"
                          : "pointer",
                      fontWeight: 600,
                      opacity: processingId === comment.id ? 0.6 : 1,
                    }}
                  >
                    ✕ Sil
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </AdminLayout>
  );
}