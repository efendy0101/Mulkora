"use client";

/**
 * SCR-ADMIN-003: Yayımlanan Gönderiler Sayfası
 *
 * Moderatörlerin onaylanmış/yayımlanan gönderileri görüntüleyeceği,
 * gerektiğinde yayından kaldırabileceği ya da düzenleme talep edebileceği panel sayfası.
 *
 * CLAUDE.md §8: Gönderi durumları — approved | takedown
 * docs/17-post-moderation-state-machine.md
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
  startAfter,
  QueryDocumentSnapshot,
  DocumentData,
  Timestamp,
} from "firebase/firestore";
import { getFunctions, httpsCallable } from "firebase/functions";
import { db } from "@/lib/firebase";
import { useAdminAuth } from "@/hooks/useAdminAuth";
import { AdminLayout } from "@/components/AdminLayout";
import toast from "react-hot-toast";
import { formatDistanceToNow } from "date-fns";
import { tr } from "date-fns/locale";

interface PublishedPost {
  id: string;
  postId: string;
  authorUid: string;
  authorUsername: string;
  title?: string;
  content: string;
  postType: string;
  category?: string;
  status: string;
  isAnonymous: boolean;
  imageUrls?: string[];
  likeCount: number;
  commentCount: number;
  averageRating?: number;
  createdAt: Timestamp;
  approvedAt?: Timestamp;
  approvedBy?: string;
}

const POST_TYPE_LABELS: Record<string, string> = {
  complaint: "Şikayet",
  experience: "Deneyim",
  question: "Soru",
  advice: "Tavsiye",
  announcement: "Duyuru",
};

const PAGE_SIZE = 20;

export default function PublishedPostsPage() {
  const { user } = useAdminAuth();
  const [lastDoc, setLastDoc] = useState<QueryDocumentSnapshot<DocumentData> | null>(null);
  const [allPosts, setAllPosts] = useState<PublishedPost[]>([]);
  const [hasMore, setHasMore] = useState(true);
  const [selectedPost, setSelectedPost] = useState<PublishedPost | null>(null);
  const [takedownReason, setTakedownReason] = useState("");
  const [isProcessing, setIsProcessing] = useState(false);
  const functions = getFunctions(undefined, "europe-west1");

  const { isLoading, refetch } = useQuery({
    queryKey: ["published-posts"],
    queryFn: async () => {
      const q = lastDoc
        ? query(
            collection(db, "posts"),
            where("status", "==", "approved"),
            orderBy("createdAt", "desc"),
            startAfter(lastDoc),
            limit(PAGE_SIZE)
          )
        : query(
            collection(db, "posts"),
            where("status", "==", "approved"),
            orderBy("createdAt", "desc"),
            limit(PAGE_SIZE)
          );
      const snapshot = await getDocs(q);
      const docs = snapshot.docs.map((doc) => ({
        id: doc.id,
        ...doc.data(),
      })) as PublishedPost[];

      setAllPosts((prev) => (lastDoc ? [...prev, ...docs] : docs));
      setHasMore(snapshot.docs.length === PAGE_SIZE);
      if (snapshot.docs.length > 0) {
        setLastDoc(snapshot.docs[snapshot.docs.length - 1]);
      }
      return docs;
    },
    enabled: !!user,
  });

  const handleTakedown = async (postId: string, reason: string) => {
    if (!reason.trim()) {
      toast.error("Yayından kaldırma nedeni zorunludur.");
      return;
    }
    setIsProcessing(true);
    try {
      const requestPostEdit = httpsCallable(functions, "requestPostEdit");
      await requestPostEdit({ postId, reason, action: "takedown" });
      toast.success("Gönderi yayından kaldırıldı.");
      setSelectedPost(null);
      setTakedownReason("");
      setAllPosts([]);
      setLastDoc(null);
      refetch();
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : "Bilinmeyen hata";
      toast.error(`Hata: ${message}`);
    } finally {
      setIsProcessing(false);
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
            <h1 style={{ margin: 0, fontSize: 24 }}>Yayımlanan Gönderiler</h1>
            <p style={{ margin: "4px 0 0", color: "#6b7280" }}>
              {allPosts.length} gönderi listeleniyor
            </p>
          </div>
          <button
            onClick={() => {
              setAllPosts([]);
              setLastDoc(null);
              setHasMore(true);
              refetch();
            }}
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

        {isLoading && allPosts.length === 0 ? (
          <div style={{ textAlign: "center", padding: 48, color: "#6b7280" }}>
            Yükleniyor...
          </div>
        ) : allPosts.length === 0 ? (
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
            Yayımlanmış gönderi bulunamadı.
          </div>
        ) : (
          <>
            <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
              {allPosts.map((post) => (
                <div
                  key={post.id}
                  style={{
                    backgroundColor: "white",
                    border: "1px solid #e5e7eb",
                    borderRadius: 10,
                    padding: 16,
                  }}
                >
                  <div
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "flex-start",
                    }}
                  >
                    <div style={{ flex: 1 }}>
                      <div style={{ display: "flex", gap: 6, marginBottom: 6 }}>
                        <span
                          style={{
                            backgroundColor: "#dcfce7",
                            color: "#16a34a",
                            padding: "2px 8px",
                            borderRadius: 12,
                            fontSize: 11,
                            fontWeight: 600,
                          }}
                        >
                          Yayımlandı
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
                      </div>
                      {post.title && (
                        <div style={{ fontWeight: 600, marginBottom: 2 }}>{post.title}</div>
                      )}
                      <div style={{ fontSize: 13, color: "#6b7280", marginBottom: 4 }}>
                        @{post.authorUsername}
                      </div>
                      <div
                        style={{
                          fontSize: 13,
                          color: "#374151",
                          overflow: "hidden",
                          display: "-webkit-box",
                          WebkitLineClamp: 2,
                          WebkitBoxOrient: "vertical",
                        }}
                      >
                        {post.content}
                      </div>
                      <div
                        style={{
                          display: "flex",
                          gap: 12,
                          marginTop: 8,
                          fontSize: 12,
                          color: "#9ca3af",
                        }}
                      >
                        <span>👍 {post.likeCount ?? 0}</span>
                        <span>💬 {post.commentCount ?? 0}</span>
                        {post.averageRating && <span>⭐ {post.averageRating.toFixed(1)}</span>}
                        <span>
                          {post.createdAt &&
                            formatDistanceToNow(post.createdAt.toDate(), {
                              addSuffix: true,
                              locale: tr,
                            })}
                        </span>
                      </div>
                    </div>
                    <button
                      onClick={() => setSelectedPost(post)}
                      style={{
                        marginLeft: 16,
                        padding: "6px 12px",
                        backgroundColor: "#fef2f2",
                        color: "#dc2626",
                        border: "1px solid #fca5a5",
                        borderRadius: 6,
                        cursor: "pointer",
                        fontSize: 12,
                        fontWeight: 600,
                        whiteSpace: "nowrap",
                      }}
                    >
                      Yayından Kaldır
                    </button>
                  </div>
                </div>
              ))}
            </div>

            {hasMore && (
              <div style={{ textAlign: "center", marginTop: 20 }}>
                <button
                  onClick={() => refetch()}
                  disabled={isLoading}
                  style={{
                    padding: "10px 24px",
                    border: "1px solid #d1d5db",
                    borderRadius: 8,
                    background: "white",
                    cursor: "pointer",
                  }}
                >
                  {isLoading ? "Yükleniyor..." : "Daha Fazla Yükle"}
                </button>
              </div>
            )}
          </>
        )}
      </div>

      {/* Yayından Kaldır Modalı */}
      {selectedPost && (
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
              maxWidth: 480,
              width: "90%",
            }}
          >
            <h2 style={{ margin: "0 0 8px", fontSize: 18 }}>Yayından Kaldır</h2>
            <p style={{ margin: "0 0 16px", color: "#6b7280", fontSize: 14 }}>
              <strong>@{selectedPost.authorUsername}</strong> kullanıcısına ait gönderi yayından
              kaldırılacak.
            </p>

            {selectedPost.title && (
              <div
                style={{
                  backgroundColor: "#f9fafb",
                  border: "1px solid #e5e7eb",
                  borderRadius: 8,
                  padding: 10,
                  marginBottom: 16,
                  fontSize: 14,
                  fontWeight: 600,
                }}
              >
                {selectedPost.title}
              </div>
            )}

            <label
              style={{ fontSize: 13, color: "#374151", display: "block", marginBottom: 4 }}
            >
              Neden <span style={{ color: "#dc2626" }}>*</span>
            </label>
            <textarea
              value={takedownReason}
              onChange={(e) => setTakedownReason(e.target.value)}
              placeholder="Yayından kaldırma nedenini açıklayın..."
              rows={3}
              style={{
                width: "100%",
                padding: 8,
                borderRadius: 6,
                border: "1px solid #d1d5db",
                resize: "vertical",
                boxSizing: "border-box",
                marginBottom: 16,
              }}
            />

            <div style={{ display: "flex", gap: 8 }}>
              <button
                onClick={() => {
                  setSelectedPost(null);
                  setTakedownReason("");
                }}
                style={{
                  flex: 1,
                  padding: "10px",
                  border: "1px solid #d1d5db",
                  borderRadius: 8,
                  background: "white",
                  cursor: "pointer",
                }}
              >
                İptal
              </button>
              <button
                onClick={() => handleTakedown(selectedPost.postId, takedownReason)}
                disabled={isProcessing}
                style={{
                  flex: 1,
                  padding: "10px",
                  backgroundColor: "#dc2626",
                  color: "white",
                  border: "none",
                  borderRadius: 8,
                  cursor: isProcessing ? "not-allowed" : "pointer",
                  fontWeight: 600,
                }}
              >
                {isProcessing ? "İşleniyor..." : "Yayından Kaldır"}
              </button>
            </div>
          </div>
        </div>
      )}
    </AdminLayout>
  );
}