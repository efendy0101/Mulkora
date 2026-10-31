"use client";

/**
 * SCR-ADMIN-007: Kategori Yönetimi Sayfası
 *
 * CLAUDE.md §7: Kategoriler yönetim panelinden yönetilebilir olmalıdır.
 * CLAUDE.md §3.5: Yönetici kategorileri yönetebilmelidir.
 * docs/11-firebase-collections.md — categories koleksiyonu.
 */

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  collection,
  query,
  orderBy,
  getDocs,
  addDoc,
  updateDoc,
  deleteDoc,
  doc,
  serverTimestamp,
  Timestamp,
} from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAdminAuth } from "@/hooks/useAdminAuth";
import { AdminLayout } from "@/components/AdminLayout";
import toast from "react-hot-toast";

interface Category {
  id: string;
  name: string;
  slug: string;
  description?: string;
  isActive: boolean;
  postCount?: number;
  createdAt: Timestamp;
}

const DEFAULT_CATEGORIES = [
  "Kira artışı",
  "Depozito",
  "Aidat",
  "Ev bakımı",
  "Tadilat",
  "Gürültü",
  "Komşuluk",
  "Tahliye",
  "Kira sözleşmesi",
  "Ev sahibi ve kiracı iletişimi",
  "Faturalar",
  "Taşınma",
  "Hasar ve bakım",
  "Genel",
];

function toSlug(name: string) {
  return name
    .toLowerCase()
    .replace(/ğ/g, "g")
    .replace(/ü/g, "u")
    .replace(/ş/g, "s")
    .replace(/ı/g, "i")
    .replace(/ö/g, "o")
    .replace(/ç/g, "c")
    .replace(/\s+/g, "_")
    .replace(/[^a-z0-9_]/g, "");
}

interface CategoryFormProps {
  initial?: Partial<Category>;
  onSubmit: (data: { name: string; description: string }) => void;
  onCancel: () => void;
  loading: boolean;
}

function CategoryForm({ initial, onSubmit, onCancel, loading }: CategoryFormProps) {
  const [name, setName] = useState(initial?.name ?? "");
  const [description, setDescription] = useState(initial?.description ?? "");

  return (
    <div
      style={{
        backgroundColor: "#f9fafb",
        border: "1px solid #e5e7eb",
        borderRadius: 8,
        padding: 16,
        marginBottom: 16,
      }}
    >
      <div style={{ marginBottom: 12 }}>
        <label style={{ fontSize: 13, fontWeight: 600, display: "block", marginBottom: 4 }}>
          Kategori Adı <span style={{ color: "#dc2626" }}>*</span>
        </label>
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Örn: Kira artışı"
          style={{
            width: "100%",
            padding: "8px 12px",
            border: "1px solid #d1d5db",
            borderRadius: 6,
            boxSizing: "border-box",
          }}
        />
        {name && (
          <div style={{ fontSize: 11, color: "#6b7280", marginTop: 4 }}>
            Slug: {toSlug(name)}
          </div>
        )}
      </div>
      <div style={{ marginBottom: 12 }}>
        <label style={{ fontSize: 13, fontWeight: 600, display: "block", marginBottom: 4 }}>
          Açıklama
        </label>
        <input
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="İsteğe bağlı açıklama"
          style={{
            width: "100%",
            padding: "8px 12px",
            border: "1px solid #d1d5db",
            borderRadius: 6,
            boxSizing: "border-box",
          }}
        />
      </div>
      <div style={{ display: "flex", gap: 8 }}>
        <button
          onClick={() => onSubmit({ name, description })}
          disabled={loading || !name.trim()}
          style={{
            padding: "8px 16px",
            backgroundColor: "#1d4ed8",
            color: "white",
            border: "none",
            borderRadius: 6,
            cursor: loading || !name.trim() ? "not-allowed" : "pointer",
            fontWeight: 600,
            opacity: loading || !name.trim() ? 0.6 : 1,
          }}
        >
          {loading ? "Kaydediliyor..." : "Kaydet"}
        </button>
        <button
          onClick={onCancel}
          style={{
            padding: "8px 16px",
            backgroundColor: "white",
            border: "1px solid #d1d5db",
            borderRadius: 6,
            cursor: "pointer",
          }}
        >
          İptal
        </button>
      </div>
    </div>
  );
}

export default function CategoriesPage() {
  const { user } = useAdminAuth();
  const queryClient = useQueryClient();
  const [showAdd, setShowAdd] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);

  const { data: categories = [], isLoading } = useQuery({
    queryKey: ["categories"],
    queryFn: async () => {
      const q = query(collection(db, "categories"), orderBy("name", "asc"));
      const snapshot = await getDocs(q);
      return snapshot.docs.map((d) => ({
        id: d.id,
        ...d.data(),
      })) as Category[];
    },
    enabled: !!user,
  });

  const addMutation = useMutation({
    mutationFn: async (data: { name: string; description: string }) => {
      await addDoc(collection(db, "categories"), {
        name: data.name.trim(),
        slug: toSlug(data.name),
        description: data.description.trim(),
        isActive: true,
        postCount: 0,
        createdAt: serverTimestamp(),
        createdBy: user?.uid,
      });
    },
    onSuccess: () => {
      toast.success("Kategori eklendi.");
      setShowAdd(false);
      queryClient.invalidateQueries({ queryKey: ["categories"] });
    },
    onError: (e: Error) => toast.error(`Hata: ${e.message}`),
  });

  const updateMutation = useMutation({
    mutationFn: async ({
      id,
      data,
    }: {
      id: string;
      data: { name: string; description: string };
    }) => {
      await updateDoc(doc(db, "categories", id), {
        name: data.name.trim(),
        slug: toSlug(data.name),
        description: data.description.trim(),
        updatedAt: serverTimestamp(),
        updatedBy: user?.uid,
      });
    },
    onSuccess: () => {
      toast.success("Kategori güncellendi.");
      setEditingId(null);
      queryClient.invalidateQueries({ queryKey: ["categories"] });
    },
    onError: (e: Error) => toast.error(`Hata: ${e.message}`),
  });

  const toggleActiveMutation = useMutation({
    mutationFn: async ({ id, isActive }: { id: string; isActive: boolean }) => {
      await updateDoc(doc(db, "categories", id), {
        isActive: !isActive,
        updatedAt: serverTimestamp(),
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["categories"] });
    },
    onError: (e: Error) => toast.error(`Hata: ${e.message}`),
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => {
      await deleteDoc(doc(db, "categories", id));
    },
    onSuccess: () => {
      toast.success("Kategori silindi.");
      queryClient.invalidateQueries({ queryKey: ["categories"] });
    },
    onError: (e: Error) => toast.error(`Hata: ${e.message}`),
  });

  const isAdmin = user?.role === "admin";

  return (
    <AdminLayout>
      <div style={{ maxWidth: 800, margin: "0 auto" }}>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            marginBottom: 24,
          }}
        >
          <div>
            <h1 style={{ margin: 0, fontSize: 24 }}>Kategoriler</h1>
            <p style={{ margin: "4px 0 0", color: "#6b7280" }}>
              {categories.length} kategori
            </p>
          </div>
          {isAdmin && !showAdd && (
            <button
              onClick={() => setShowAdd(true)}
              style={{
                padding: "10px 20px",
                backgroundColor: "#1d4ed8",
                color: "white",
                border: "none",
                borderRadius: 8,
                cursor: "pointer",
                fontWeight: 600,
              }}
            >
              + Kategori Ekle
            </button>
          )}
        </div>

        {showAdd && (
          <CategoryForm
            onSubmit={(data) => addMutation.mutate(data)}
            onCancel={() => setShowAdd(false)}
            loading={addMutation.isPending}
          />
        )}

        {isLoading ? (
          <div style={{ textAlign: "center", padding: 48, color: "#6b7280" }}>
            Yükleniyor...
          </div>
        ) : (
          <div
            style={{
              backgroundColor: "white",
              border: "1px solid #e5e7eb",
              borderRadius: 12,
              overflow: "hidden",
            }}
          >
            {categories.map((cat, idx) => (
              <div key={cat.id}>
                {editingId === cat.id ? (
                  <div style={{ padding: 16 }}>
                    <CategoryForm
                      initial={cat}
                      onSubmit={(data) =>
                        updateMutation.mutate({ id: cat.id, data })
                      }
                      onCancel={() => setEditingId(null)}
                      loading={updateMutation.isPending}
                    />
                  </div>
                ) : (
                  <div
                    style={{
                      display: "flex",
                      alignItems: "center",
                      padding: "14px 20px",
                      borderBottom:
                        idx < categories.length - 1
                          ? "1px solid #e5e7eb"
                          : "none",
                      opacity: cat.isActive ? 1 : 0.5,
                    }}
                  >
                    <div style={{ flex: 1 }}>
                      <div style={{ fontWeight: 600 }}>{cat.name}</div>
                      <div style={{ fontSize: 12, color: "#6b7280" }}>
                        /{cat.slug}
                        {cat.description && ` — ${cat.description}`}
                      </div>
                    </div>
                    <div
                      style={{
                        fontSize: 12,
                        color: "#6b7280",
                        marginRight: 16,
                      }}
                    >
                      {cat.postCount ?? 0} gönderi
                    </div>
                    {isAdmin && (
                      <div style={{ display: "flex", gap: 8 }}>
                        <button
                          onClick={() =>
                            toggleActiveMutation.mutate({
                              id: cat.id,
                              isActive: cat.isActive,
                            })
                          }
                          style={{
                            padding: "4px 10px",
                            border: "1px solid #d1d5db",
                            borderRadius: 6,
                            background: cat.isActive ? "#fef2f2" : "#f0fdf4",
                            color: cat.isActive ? "#dc2626" : "#16a34a",
                            cursor: "pointer",
                            fontSize: 12,
                          }}
                        >
                          {cat.isActive ? "Devre Dışı" : "Etkinleştir"}
                        </button>
                        <button
                          onClick={() => setEditingId(cat.id)}
                          style={{
                            padding: "4px 10px",
                            border: "1px solid #d1d5db",
                            borderRadius: 6,
                            background: "white",
                            cursor: "pointer",
                            fontSize: 12,
                          }}
                        >
                          Düzenle
                        </button>
                        <button
                          onClick={() => {
                            if (
                              confirm(
                                `"${cat.name}" kategorisini silmek istediğinizden emin misiniz?`
                              )
                            ) {
                              deleteMutation.mutate(cat.id);
                            }
                          }}
                          style={{
                            padding: "4px 10px",
                            border: "1px solid #fca5a5",
                            borderRadius: 6,
                            background: "#fef2f2",
                            color: "#dc2626",
                            cursor: "pointer",
                            fontSize: 12,
                          }}
                        >
                          Sil
                        </button>
                      </div>
                    )}
                  </div>
                )}
              </div>
            ))}
            {categories.length === 0 && (
              <div
                style={{ padding: 32, textAlign: "center", color: "#6b7280" }}
              >
                Henüz kategori eklenmemiş.
                <div style={{ marginTop: 8, fontSize: 13 }}>
                  Önerilen kategoriler: {DEFAULT_CATEGORIES.join(", ")}
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </AdminLayout>
  );
}