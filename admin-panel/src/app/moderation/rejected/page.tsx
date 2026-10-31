"use client";

/**
 * SCR-ADMIN-004: Reddedilen Gönderiler Sayfası
 *
 * Moderatörlerin reddedilmiş gönderileri arşivlediği, reddetme nedenlerini
 * inceleyebildiği panel sayfası.
 *
 * CLAUDE.md §8: Gönderi durumları — rejected
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
import { db } from "@/lib/firebase";
import { useAdminAuth } from "@/hooks/useAdminAuth";
import { AdminLayout } from "@/components/AdminLayout";
import { formatDistanceToNow } from "date-fns";
import { tr } from "date-fns/locale";

interface RejectedPost {
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
  rejectionReason?: string;
  rejectedBy?: string;
  createdAt: Timestamp;
  rejectedAt?: Timestamp;
}

const POST_TYPE_LABELS: Record<string, string> = {
  complaint: "Şikayet",
  experience: "Deneyim",
  question: "Soru",
  advice: "Tavsiye",
  announcement: "Duyuru",
};

const PAGE_SIZE = 20;

export default function RejectedPostsPage() {
  const { user } = useAdminAuth();
  const [lastDoc, setLastDoc] = useState<QueryDocumentSnapshot<DocumentData> | null>(null);
  const [allPosts, setAllPosts] = useState<RejectedPost[]>([]);
  const [hasMore, setHasMore] = useState(true);
  const [expandedId, setExpandedId] = useState<string | null>(null);

  const { isLoading, refetch } = useQuery({
    queryKey: ["rejected-posts"],
    queryFn: async () => {
      const q = lastDoc
        ? query(
            collection(db, "moderationQueue"),
            where("status", "==", "rejected"),
            orderBy("createdAt", "desc"),
            startAfter(lastDoc),
            limit(PAGE_SIZE)
          )
        : query(
            collection(db, "moderationQueue"),
            where("status", "==", "rejected"),
            orderBy("createdAt", "desc"),
            limit(PAGE_SIZE)
          );
      const snapshot = await getDocs(q);
      const docs = snapshot.docs.map((doc) => ({
        id: doc.id,
        ...doc.data(),
      })) as RejectedPost[];

      setAllPosts((prev) => (lastDoc ? [...prev, ...docs] : docs));
      setHasMore(snapshot.docs.length === PAGE_SIZE);
      if (snapshot.docs.length > 0) {
        setLastDoc(snapshot.docs[snapshot.docs.length - 1]);
      }
      return docs;
    },
    enabled: !!user,
  });

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
            <h1 style={{ margin: 0, fontSize: 24 }}>Reddedilen Gönderiler</h1>
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
            Reddedilmiş gönderi bulunamadı.
          </div>
        ) : (
          <>
            <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
              {allPosts.map((post) => (
                <div
                  key={post.id}
                  style={{
                    backgroundColor: "white",
                    border: "1px solid #fecaca",
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
                            backgroundColor: "#fee2e2",
                            color: "#dc2626",
                            padding: "2px 8px",
                            borderRadius: 12,
                            fontSize: 11,
                            fontWeight: 600,
                          }}
                        >
                          Reddedildi
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

                      {expandedId === post.id ? (
                        <div style={{ fontSize: 13, color: "#374151", marginBottom: 8 }}>
                          {post.content}
                        </div>
                      ) : (
                        <div
                          style={{
                            fontSize: 13,
                            color: "#374151",
                            overflow: "hidden",
                            display: "-webkit-box",
                            WebkitLineClamp: 2,
                            WebkitBoxOrient: "vertical",
                            marginBottom: 4,
                          }}
                        >
                          {post.content}
                        </div>
                      )}

                      {post.rejectionReason && (
                        <div
                          style={{
                            backgroundColor: "#fef2f2",
                            border: "1px solid #fecaca",
                            borderRadius: 6,
                            padding: "6px 10px",
                            fontSize: 12,
                            color: "#991b1b",
                            marginTop: 6,
                          }}
                        >
                          <strong>Red nedeni:</strong> {post.rejectionReason}
                        </div>
                      )}

                      <div
                        style={{
                          display: "flex",
                          gap: 12,
                          marginTop: 8,
                          fontSize: 12,
                          color: "#9ca3af",
                        }}
                      >
                        <span>
                          {post.createdAt &&
                            formatDistanceToNow(post.createdAt.toDate(), {
                              addSuffix: true,
                              locale: tr,
                            })}
                        </span>
                        {post.rejectedBy && <span>Reddeden: {post.rejectedBy}</span>}
                      </div>
                    </div>
                    <button
                      onClick={() =>
                        setExpandedId(expandedId === post.id ? null : post.id)
                      }
                      style={{
                        marginLeft: 16,
                        padding: "4px 10px",
                        border: "1px solid #d1d5db",
                        borderRadius: 6,
                        background: "white",
                        cursor: "pointer",
                        fontSize: 12,
                        color: "#374151",
                        whiteSpace: "nowrap",
                      }}
                    >
                      {expandedId === post.id ? "Daralt" : "Genişlet"}
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
    </AdminLayout>
  );
}