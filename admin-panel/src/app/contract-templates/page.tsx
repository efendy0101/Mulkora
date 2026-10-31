"use client";

/**
 * SCR-ADMIN-008: Sözleşme Şablonları Yönetim Sayfası
 *
 * CLAUDE.md §14: Sözleşme şablonu seçilir, bilgiler doldurulur.
 * CLAUDE.md §3.5: Yönetici sözleşme şablonlarını yönetebilmelidir.
 * docs/11-firebase-collections.md — contractTemplates koleksiyonu.
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

interface ContractTemplate {
  id: string;
  name: string;
  description?: string;
  content: string;
  version: number;
  isActive: boolean;
  isDefault: boolean;
  createdAt: Timestamp;
  updatedAt?: Timestamp;
}

const STARTER_TEMPLATE = `KONUT KİRA SÖZLEŞMESİ

Madde 1 — Taraflar
Kiraya Veren: {{ev_sahibi_adi}} (TC: {{ev_sahibi_tc}})
Kiracı: {{kiraci_adi}} (TC: {{kiraci_tc}})

Madde 2 — Kiralanan Taşınmaz
Adres: {{adres}}
Taşınmaz Türü: {{tasınmaz_turu}}

Madde 3 — Kira Bedeli ve Ödeme
Aylık Kira: {{kira_bedeli}} TL
Ödeme Günü: Her ayın {{odeme_gunu}}. günü
Ödeme Yöntemi: {{odeme_yontemi}}

Madde 4 — Kira Süresi
Başlangıç Tarihi: {{baslangic_tarihi}}
Bitiş Tarihi: {{bitis_tarihi}}

Madde 5 — Depozito
Depozito Tutarı: {{depozito}} TL

Madde 6 — Aidat ve Faturalar
Aidat: {{aidat_bilgisi}}
Fatura Sorumlulukları: {{fatura_sorumlulukları}}

Madde 7 — Özel Şartlar
{{ozel_sartlar}}

Madde 8 — Kefil Bilgileri
{{kefil_bilgileri}}

Madde 9 — Ek Açıklamalar
{{ek_aciklamalar}}

Yukarıdaki koşulları kabul ediyoruz.

Tarih: {{sozlesme_tarihi}}
Belge No: {{belge_no}}
Doğrulama Kodu: {{dogrulama_kodu}}`;

function TemplateEditor({
  initial,
  onSave,
  onCancel,
  loading,
}: {
  initial?: Partial<ContractTemplate>;
  onSave: (data: { name: string; description: string; content: string }) => void;
  onCancel: () => void;
  loading: boolean;
}) {
  const [name, setName] = useState(initial?.name ?? "");
  const [description, setDescription] = useState(initial?.description ?? "");
  const [content, setContent] = useState(initial?.content ?? STARTER_TEMPLATE);

  return (
    <div
      style={{
        backgroundColor: "white",
        border: "1px solid #e5e7eb",
        borderRadius: 12,
        padding: 24,
        marginBottom: 24,
      }}
    >
      <h3 style={{ margin: "0 0 16px" }}>
        {initial?.id ? "Şablonu Düzenle" : "Yeni Şablon"}
      </h3>
      <div style={{ marginBottom: 12 }}>
        <label style={{ fontSize: 13, fontWeight: 600, display: "block", marginBottom: 4 }}>
          Şablon Adı <span style={{ color: "#dc2626" }}>*</span>
        </label>
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Örn: Standart Konut Kira Sözleşmesi"
          style={{
            width: "100%",
            padding: "8px 12px",
            border: "1px solid #d1d5db",
            borderRadius: 6,
            boxSizing: "border-box",
          }}
        />
      </div>
      <div style={{ marginBottom: 12 }}>
        <label style={{ fontSize: 13, fontWeight: 600, display: "block", marginBottom: 4 }}>
          Açıklama
        </label>
        <input
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="Şablon hakkında kısa açıklama"
          style={{
            width: "100%",
            padding: "8px 12px",
            border: "1px solid #d1d5db",
            borderRadius: 6,
            boxSizing: "border-box",
          }}
        />
      </div>
      <div style={{ marginBottom: 16 }}>
        <label style={{ fontSize: 13, fontWeight: 600, display: "block", marginBottom: 4 }}>
          Şablon İçeriği <span style={{ color: "#dc2626" }}>*</span>
        </label>
        <p style={{ fontSize: 12, color: "#6b7280", marginTop: 0, marginBottom: 8 }}>
          Değişkenler {"{{degisken_adi}}"} formatında yazılmalıdır.
        </p>
        <textarea
          value={content}
          onChange={(e) => setContent(e.target.value)}
          rows={20}
          style={{
            width: "100%",
            padding: "8px 12px",
            border: "1px solid #d1d5db",
            borderRadius: 6,
            fontFamily: "monospace",
            fontSize: 13,
            lineHeight: 1.6,
            boxSizing: "border-box",
            resize: "vertical",
          }}
        />
      </div>
      <div style={{ display: "flex", gap: 8 }}>
        <button
          onClick={() => onSave({ name, description, content })}
          disabled={loading || !name.trim() || !content.trim()}
          style={{
            padding: "10px 20px",
            backgroundColor: "#1d4ed8",
            color: "white",
            border: "none",
            borderRadius: 8,
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
            padding: "10px 20px",
            backgroundColor: "white",
            border: "1px solid #d1d5db",
            borderRadius: 8,
            cursor: "pointer",
          }}
        >
          İptal
        </button>
      </div>
    </div>
  );
}

export default function ContractTemplatesPage() {
  const { user } = useAdminAuth();
  const queryClient = useQueryClient();
  const [showAdd, setShowAdd] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);

  const { data: templates = [], isLoading } = useQuery({
    queryKey: ["contract-templates"],
    queryFn: async () => {
      const q = query(
        collection(db, "contractTemplates"),
        orderBy("createdAt", "desc")
      );
      const snapshot = await getDocs(q);
      return snapshot.docs.map((d) => ({
        id: d.id,
        ...d.data(),
      })) as ContractTemplate[];
    },
    enabled: !!user,
  });

  const addMutation = useMutation({
    mutationFn: async (data: {
      name: string;
      description: string;
      content: string;
    }) => {
      await addDoc(collection(db, "contractTemplates"), {
        name: data.name.trim(),
        description: data.description.trim(),
        content: data.content.trim(),
        version: 1,
        isActive: true,
        isDefault: templates.length === 0,
        createdAt: serverTimestamp(),
        createdBy: user?.uid,
      });
    },
    onSuccess: () => {
      toast.success("Şablon eklendi.");
      setShowAdd(false);
      queryClient.invalidateQueries({ queryKey: ["contract-templates"] });
    },
    onError: (e: Error) => toast.error(`Hata: ${e.message}`),
  });

  const updateMutation = useMutation({
    mutationFn: async ({
      id,
      data,
      currentVersion,
    }: {
      id: string;
      data: { name: string; description: string; content: string };
      currentVersion: number;
    }) => {
      await updateDoc(doc(db, "contractTemplates", id), {
        name: data.name.trim(),
        description: data.description.trim(),
        content: data.content.trim(),
        version: currentVersion + 1,
        updatedAt: serverTimestamp(),
        updatedBy: user?.uid,
      });
    },
    onSuccess: () => {
      toast.success("Şablon güncellendi.");
      setEditingId(null);
      queryClient.invalidateQueries({ queryKey: ["contract-templates"] });
    },
    onError: (e: Error) => toast.error(`Hata: ${e.message}`),
  });

  const setDefaultMutation = useMutation({
    mutationFn: async (id: string) => {
      const batch = templates.map((t) =>
        updateDoc(doc(db, "contractTemplates", t.id), {
          isDefault: t.id === id,
        })
      );
      await Promise.all(batch);
    },
    onSuccess: () => {
      toast.success("Varsayılan şablon güncellendi.");
      queryClient.invalidateQueries({ queryKey: ["contract-templates"] });
    },
    onError: (e: Error) => toast.error(`Hata: ${e.message}`),
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => {
      await deleteDoc(doc(db, "contractTemplates", id));
    },
    onSuccess: () => {
      toast.success("Şablon silindi.");
      queryClient.invalidateQueries({ queryKey: ["contract-templates"] });
    },
    onError: (e: Error) => toast.error(`Hata: ${e.message}`),
  });

  const isAdmin = user?.role === "admin";

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
            <h1 style={{ margin: 0, fontSize: 24 }}>Sözleşme Şablonları</h1>
            <p style={{ margin: "4px 0 0", color: "#6b7280" }}>
              {templates.length} şablon
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
              + Yeni Şablon
            </button>
          )}
        </div>

        {showAdd && (
          <TemplateEditor
            onSave={(data) => addMutation.mutate(data)}
            onCancel={() => setShowAdd(false)}
            loading={addMutation.isPending}
          />
        )}

        {isLoading ? (
          <div style={{ textAlign: "center", padding: 48, color: "#6b7280" }}>
            Yükleniyor...
          </div>
        ) : templates.length === 0 && !showAdd ? (
          <div
            style={{
              backgroundColor: "#eff6ff",
              border: "1px solid #bfdbfe",
              borderRadius: 12,
              padding: 32,
              textAlign: "center",
              color: "#1d4ed8",
            }}
          >
            <div style={{ fontSize: 32, marginBottom: 12 }}>📄</div>
            <div style={{ fontWeight: 600 }}>Henüz şablon yok</div>
            <div style={{ marginTop: 8, color: "#6b7280", fontSize: 13 }}>
              Kullanıcıların sözleşme oluşturabilmesi için en az bir şablon
              gereklidir.
            </div>
          </div>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
            {templates.map((tmpl) =>
              editingId === tmpl.id ? (
                <TemplateEditor
                  key={tmpl.id}
                  initial={tmpl}
                  onSave={(data) =>
                    updateMutation.mutate({
                      id: tmpl.id,
                      data,
                      currentVersion: tmpl.version,
                    })
                  }
                  onCancel={() => setEditingId(null)}
                  loading={updateMutation.isPending}
                />
              ) : (
                <div
                  key={tmpl.id}
                  style={{
                    backgroundColor: "white",
                    border: tmpl.isDefault
                      ? "2px solid #1d4ed8"
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
                    <div>
                      <div
                        style={{
                          display: "flex",
                          gap: 8,
                          alignItems: "center",
                          marginBottom: 4,
                        }}
                      >
                        <span style={{ fontWeight: 700, fontSize: 16 }}>
                          {tmpl.name}
                        </span>
                        {tmpl.isDefault && (
                          <span
                            style={{
                              backgroundColor: "#1d4ed8",
                              color: "white",
                              padding: "2px 8px",
                              borderRadius: 12,
                              fontSize: 11,
                              fontWeight: 600,
                            }}
                          >
                            Varsayılan
                          </span>
                        )}
                        {!tmpl.isActive && (
                          <span
                            style={{
                              backgroundColor: "#f3f4f6",
                              color: "#6b7280",
                              padding: "2px 8px",
                              borderRadius: 12,
                              fontSize: 11,
                            }}
                          >
                            Pasif
                          </span>
                        )}
                        <span
                          style={{
                            backgroundColor: "#f3f4f6",
                            color: "#374151",
                            padding: "2px 8px",
                            borderRadius: 12,
                            fontSize: 11,
                          }}
                        >
                          v{tmpl.version}
                        </span>
                      </div>
                      {tmpl.description && (
                        <div style={{ fontSize: 13, color: "#6b7280" }}>
                          {tmpl.description}
                        </div>
                      )}
                    </div>
                    {isAdmin && (
                      <div style={{ display: "flex", gap: 8, flexShrink: 0 }}>
                        {!tmpl.isDefault && (
                          <button
                            onClick={() => setDefaultMutation.mutate(tmpl.id)}
                            style={{
                              padding: "6px 12px",
                              border: "1px solid #d1d5db",
                              borderRadius: 6,
                              background: "white",
                              cursor: "pointer",
                              fontSize: 12,
                            }}
                          >
                            Varsayılan Yap
                          </button>
                        )}
                        <button
                          onClick={() => setEditingId(tmpl.id)}
                          style={{
                            padding: "6px 12px",
                            border: "1px solid #d1d5db",
                            borderRadius: 6,
                            background: "white",
                            cursor: "pointer",
                            fontSize: 12,
                          }}
                        >
                          Düzenle
                        </button>
                        {!tmpl.isDefault && (
                          <button
                            onClick={() => {
                              if (
                                confirm(
                                  `"${tmpl.name}" şablonunu silmek istediğinizden emin misiniz?`
                                )
                              ) {
                                deleteMutation.mutate(tmpl.id);
                              }
                            }}
                            style={{
                              padding: "6px 12px",
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
                        )}
                      </div>
                    )}
                  </div>
                  <pre
                    style={{
                      backgroundColor: "#f9fafb",
                      border: "1px solid #e5e7eb",
                      borderRadius: 6,
                      padding: 12,
                      fontSize: 12,
                      lineHeight: 1.5,
                      maxHeight: 200,
                      overflow: "auto",
                      whiteSpace: "pre-wrap",
                      wordBreak: "break-word",
                      margin: 0,
                    }}
                  >
                    {tmpl.content}
                  </pre>
                </div>
              )
            )}
          </div>
        )}
      </div>
    </AdminLayout>
  );
}