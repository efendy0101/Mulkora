"use client";

/**
 * SCR-ADMIN-002: Onay Bekleyen Gönderiler Moderasyon Sayfası
 *
 * Moderatörlerin `moderationQueue` koleksiyonundaki `status == "pending"` gönderileri
 * inceleyip onayla / reddet işlemi yapacağı panel sayfası.
 *
 * CLAUDE.md §8: Gönderi onay sistemi — approvePost / rejectPost CF.
 * docs/17-post-moderation-state-machine.md — pending → approved | rejected
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

interface PendingPost {
  id: string;
  postId: string;
  authorUid: string;
  authorUsername: string;
  title?: string;
  content: string;
  postType: string;
  category?: string;
  status: string;
  isFlagged: boolean;
  flagReason?: string;
  filterRiskLevel?: string;
  imageUrls?: string[];
  isAnonymous: boolean;
  createdAt: Timestamp;
}

interface PostDetailModalProps {
  post: PendingPost;
  onClose: () => void;
  onApprove: (postId: string, moderatorNote: string) => Promise<void>;
  onReject: (postId: string, reason: string) => Promise<void>;
}

const POST_TYPE_LABELS: Record<string, string> = {
  complaint: "Şikayet",
  experience: "Deneyim",
  question: "Soru",
  advice: "Tavsiye",
  announcement: "Duyuru",
};

function PostDetailModal({ post, onClose, onApprove, onReject }: PostDetailModalProps) {
  const [moderatorNote, setModeratorNote] = useState("");
  const [rejectReason, setRejectReason] = useState("");
  const [isProcessing, setIsProcessing] = useState(false);
  const [activeAction, setActiveAction] = useState<"approve" | "reject" | null>(null);

  const handleApprove = async () => {
    setIsProcessing(true);
    try {
      await onApprove(post.postId, moderatorNote);
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
      await onReject(post.postId, rejectReason);
      onClose();
    } finally {
      setIsProcessing(false);
    }
  };

  const riskColor =
    post.filterRiskLevel === "CRITICAL"
      ? "#dc2626"
      : post.filterRiskLevel === "HIGH"
      ? "#ea580c"
      : post.filterRiskLevel === "MEDIUM"
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
          maxWidth: 640,
          width: "90%",
          maxHeight: "90vh",
          overflowY: "auto",
        }}
      >
        <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 16 }}>
          <h2 style={{ margin: 0, fontSize: 18 }}>Gönderi İnceleme</h2>
          <button
            onClick={onClose}
            style={{ background: "none", border: "none", cursor: "pointer", fontSize: 20 }}
          >
            ✕
          </button>
        </div>

        {post.isFlagged && (
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
            <strong style={{ color: riskColor }}>{post.filterRiskLevel}</strong>
            {post.flagReason && (
              <div style={{ marginTop: 4, fontSize: 13 }}>{post.flagReason}</div>
            )}
          </div>
        )}

        <div style={{ display: "flex", gap: 8, marginBottom: 12 }}>
          <span
            style={{
              backgroundColor: "#f3f4f6",
              color: "#374151",
              padding: "2px 8px",
              borderRadius: 12,
              fontSize: 12,
              fontWeight: 600,
            }}
          >
            {POST_TYPE_LABELS[post.postType] ?? post.postType}
          </span>
          {post.category && (
            <span
              style={{
                backgroundColor: "#eff6ff",
                color: "#1d4ed8",
                padding: "2px 8px",
                borderRadius: 12,
                fontSize: 12,
              }}
            >
              {post.category}
            </span>
          )}
          {post.isAnonymous && (
            <span
              style={{
                backgroundColor: "#f3f4f6",
                color: "#6b7280",
                padding: "2px 8px",
                borderRadius: 12,
                fontSize: 12,
              }}
            >
              Anonim
            </span>
          )}
        </div>

        <div style={{ marginBottom: 12 }}>
          <div style={{ fontSize: 13, color: "#6b7280" }}>Yazar</div>
          <div style={{ fontWeight: 600 }}>@{post.authorUsername}</div>
        </div>

        {post.title && (
          <div style={{ marginBottom: 12 }}>
            <div style={{ fontSize: 13, color: "#6b7280" }}>Başlık</div>
            <div style={{ fontWeight: 600, fontSize: 16 }}>{post.title}</div>
          </div>
        )}

        <div style={{ marginBottom: 16 }}>
          <div style={{ fontSize: 13, color: "#6b7280", marginBottom: 4 }}>İçerik</div>
          <div
            style={{
              backgroundColor: "#f9fafb",
              border: "1px solid #e5e7eb",
              borderRadius: 8,
              padding: 12,
              lineHeight: 1.6,
              whiteSpace: "pre-wrap",
            }}
          >
            {post.content}
          </div>
        </div>

        {post.imageUrls && post.imageUrls.length > 0 && (
          <div style={{ marginBottom: 16 }}>
            <div style={{ fontSize: 13, color: "#6b7280", marginBottom: 4 }}>
              Görseller ({post.imageUrls.length})
            </div>
            <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
              {post.imageUrls.map((url, idx) => (
                <a key={idx} href={url} target="_blank" rel="noopener noreferrer">
                  {/* eslint-disable-next-line @next/next/no-img-element */}
                  <img
                    src={url}
                    alt={`Görsel ${idx + 1}`}
                    style={{ width: 80, height: 80, objectFit: "cover", borderRadius: 6 }}
                  />
                </a>
              ))}
            </div>
          </div>
        )}

        <div style={{ marginBottom: 20 }}>
          <div style={{ fontSize: 13, color: "#6b7280" }}>
            {formatDistanceToNow(post.createdAt.toDate(), {
              addSuffix: true,
              locale: tr,
            })}
          </div>
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
            ✓ Onayla
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
            ✕ Reddet
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
              placeholder="Reddetme nedenini açıklayın..."
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

export default function ModerationPostsPage() {
  const { user } = useAdminAuth();
  const [selectedPost, setSelectedPost] = useState<PendingPost | null>(null);
  const functions = getFunctions(undefined, "europe-west1");

  const {
    data: posts = [],
    isLoading,
    refetch,
  } = useQuery({
    queryKey: ["moderation-pending-posts"],
    queryFn: async () => {
      const q = query(
        collection(db, "moderationQueue"),
        where("status", "==", "pending"),
        orderBy("createdAt", "asc"),
        limit(50)
      );
      const snapshot = await getDocs(q);
      return snapshot.docs.map((doc) => ({
        id: doc.id,
        ...doc.data(),
      })) as PendingPost[];
    },
    enabled: !!user,
    refetchInterval: 30_000,
  });

  const handleApprove = async (postId: string, moderatorNote: string) => {
    try {
      const approvePost = httpsCallable(functions, "approvePost");
      await approvePost({ postId, moderatorNote });
      toast.success("Gönderi onaylandı.");
      refetch();
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : "Bilinmeyen hata";
      toast.error(`Hata: ${message}`);
      throw error;
    }
  };

  const handleReject = async (postId: string, reason: string) => {
    try {
      const rejectPost = httpsCallable(functions, "rejectPost");
      await rejectPost({ postId, reason });
      toast.success("Gönderi reddedildi.");
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
            <h1 style={{ margin: 0, fontSize: 24 }}>Onay Bekleyen Gönderiler</h1>
            <p style={{ margin: "4px 0 0", color: "#6b7280" }}>
              {posts.length} gönderi moderasyon bekliyor
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
        ) : posts.length === 0 ? (
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
            <div style={{ fontSize: 18, fontWeight: 600 }}>Bekleyen gönderi yok</div>
            <div style={{ marginTop: 8, color: "#6b7280" }}>Tüm gönderiler incelendi.</div>
          </div>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
            {posts.map((post) => (
              <div
                key={post.id}
                style={{
                  backgroundColor: "white",
                  border: post.isFlagged ? "2px solid #fca5a5" : "1px solid #e5e7eb",
                  borderRadius: 12,
                  padding: 20,
                  cursor: "pointer",
                  transition: "box-shadow 0.2s",
                }}
                onClick={() => setSelectedPost(post)}
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
                        marginBottom: 6,
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
                        {POST_TYPE_LABELS[post.postType] ?? post.postType}
                      </span>
                      {post.category && (
                        <span
                          style={{
                            backgroundColor: "#eff6ff",
                            color: "#1d4ed8",
                            padding: "2px 8px",
                            borderRadius: 12,
                            fontSize: 11,
                          }}
                        >
                          {post.category}
                        </span>
                      )}
                      {post.isAnonymous && (
                        <span
                          style={{
                            backgroundColor: "#f3f4f6",
                            color: "#9ca3af",
                            padding: "2px 8px",
                            borderRadius: 12,
                            fontSize: 11,
                          }}
                        >
                          Anonim
                        </span>
                      )}
                    </div>
                    {post.title && (
                      <div style={{ fontWeight: 600, marginBottom: 4 }}>{post.title}</div>
                    )}
                    <div style={{ color: "#6b7280", fontSize: 13, marginBottom: 6 }}>
                      @{post.authorUsername}
                    </div>
                    <div
                      style={{
                        color: "#374151",
                        fontSize: 14,
                        overflow: "hidden",
                        display: "-webkit-box",
                        WebkitLineClamp: 2,
                        WebkitBoxOrient: "vertical",
                      }}
                    >
                      {post.content}
                    </div>
                  </div>
                  <div
                    style={{
                      display: "flex",
                      flexDirection: "column",
                      alignItems: "flex-end",
                      gap: 6,
                      marginLeft: 16,
                    }}
                  >
                    {post.isFlagged && (
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
                        {post.filterRiskLevel} RİSK
                      </span>
                    )}
                    {post.imageUrls && post.imageUrls.length > 0 && (
                      <span style={{ fontSize: 12, color: "#6b7280" }}>
                        📷 {post.imageUrls.length} görsel
                      </span>
                    )}
                    <span style={{ fontSize: 12, color: "#9ca3af" }}>
                      {formatDistanceToNow(post.createdAt.toDate(), {
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

      {selectedPost && (
        <PostDetailModal
          post={selectedPost}
          onClose={() => setSelectedPost(null)}
          onApprove={handleApprove}
          onReject={handleReject}
        />
      )}
    </AdminLayout>
  );
}