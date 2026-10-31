"use client";

/**
 * SCR-ADMIN-011: Yasaklı Kelimeler Yönetim Sayfası
 *
 * CLAUDE.md §12: İçerik filtresi için yasaklı kelimeler ve telefon/IBAN tespiti.
 * docs/18-content-filter-design.md — bannedTerms koleksiyonu.
 * Yalnızca adminler erişebilir.
 */

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  collection,
  query,
  orderBy,
  getDocs,
  addDoc,
  deleteDoc,
  doc,
  serverTimestamp,
  Timestamp,
} from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAdminAuth } from "@/hooks/useAdminAuth";
import { AdminLayout } from "@/components/AdminLayout";
import toast from "react-hot-toast";

interface BannedTerm {
  id: string;
  term: string;
  category: "profanity" | "phone" | "iban" | "address" | "custom";
  riskLevel: 1 | 2 | 3 | 4;
  isRegex: boolean;
  addedBy?: string;
  createdAt: Timestamp;
}

const CATEGORY_LABELS: Record<string, { label: string; color: string }> = {
  profanity: { label: "Küfür / Hakaret", color: "#dc2626" },
  phone: { label: "Telefon Numarası", color: "#ea580c" },
  iban: { label: "IBAN / Banka", color: "#ca8a04" },
  address: { label: "Adres", color: "#16a34a" },
  custom: { label: "Özel", color: "#7c3aed" },
};

const RISK_LABELS: Record<number, { label: string; color: string; bg: string }> = {
  1: { label: "Düşük", color: "#16a34a", bg: "#f0fdf4" },
  2: { label: "Orta", color: "#ca8a04", bg: "#fefce8" },
  3: { label: "Yüksek", color: "#ea580c", bg: "#fff7ed" },
  4: { label: "Kritik", color: "#dc2626", bg: "#fef2f2" },
};

export default function BannedTermsPage() {
  const { user } = useAdminAuth();
  const queryClient = useQueryClient();
  const [showAdd, setShowAdd] = useState(false);
  const [newTerm, setNewTerm] = useState("");
  const [newCategory, setNewCategory] = useState<BannedTerm["category"]>("profanity");
  const [newRisk, setNewRisk] = useState<1 | 2 | 3 | 4>(2);
  const [newIsRegex, setNewIsRegex] = useState(false);
  const [filterCategory, setFilterCategory] = useState("all");
  const [searchQuery, setSearchQuery] = useState("");

  const { data: terms = [], isLoading } = useQuery({
    queryKey: ["banned-terms"],
    queryFn: async () => {
      const q = query(
        collection(db, "bannedTerms"),
        orderBy("createdAt", "desc")
      );
      const snapshot = await getDocs(q);
      return snapshot.docs.map((d) => ({
        id: d.id,
        ...d.data(),
      })) as BannedTerm[];
    },
    enabled: !!user && user.role === "admin",
  });

  const addMutation = useMutation({
    mutationFn: async () => {
      if (!newTerm.trim()) throw new Error("Terim boş olamaz");
      await addDoc(collection(db, "bannedTerms"), {
        term: newTerm.trim(),
        category: newCategory,
        riskLevel: newRisk,
        isRegex: newIsRegex,
        addedBy: user?.uid,
        createdAt: serverTimestamp(),
      });
    },
    onSuccess: () => {
      toast.success("Yasaklı terim eklendi.");
      setNewTerm("");
      setShowAdd(false);
      queryClient.invalidateQueries({ queryKey: ["banned-terms"] });
    },
    onError: (e: Error) => toast.error(`Hata: ${e.message}`),
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => {
      await deleteDoc(doc(db, "bannedTerms", id));
    },
    onSuccess: () => {
      toast.success("Terim kaldırıldı.");
      queryClient.invalidateQueries({ queryKey: ["banned-terms"] });
    },
    onError: (e: Error) => toast.error(`Hata: ${e.message}`),
  });

  if (user && user.role !== "admin") {
    return (
      <AdminLayout>
        <div style={{ textAlign: "center", padding: 48, color: "#dc2626" }}>
          <div style={{ fontSize: 48, marginBottom: 16 }}>🔒</div>
          <div style={{ fontSize: 18, fontWeight: 600 }}>
            Bu sayfaya erişim yetkiniz yok.
          </div>
        </div>
      </AdminLayout>
    );
  }

  const filteredTerms = terms.filter((t) => {
    const matchesCategory =
      filterCategory === "all" || t.category === filterCategory;
    const matchesSearch =
      !searchQuery ||
      t.term.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesCategory && matchesSearch;
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
            <h1 style={{ margin: 0, fontSize: 24 }}>Yasaklı Kelimeler</h1>
            <p style={{ margin: "4px 0 0", color: "#6b7280" }}>
              {terms.length} terim tanımlanmış
            </p>
          </div>
          {!showAdd && (
            <button
              onClick={() => setShowAdd(true)}
              style={{
                padding: "10px 20px",
                backgroundColor: "#dc2626",
                color: "white",
                border: "none",
                borderRadius: 8,
                cursor: "pointer",
                fontWeight: 600,
              }}
            >
              + Terim Ekle
            </button>
          )}
        </div>

        {showAdd && (
          <div
            style={{
              backgroundColor: "white",
              border: "1px solid #e5e7eb",
              borderRadius: 12,
              padding: 20,
              marginBottom: 20,
            }}
          >
            <h3 style={{ margin: "0 0 16px" }}>Yeni Yasaklı Terim</h3>
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "1fr 1fr 1fr",
                gap: 12,
                marginBottom: 12,
              }}
            >
              <div>
                <label
                  style={{
                    fontSize: 12,
                    fontWeight: 600,
                    display: "block",
                    marginBottom: 4,
                  }}
                >
                  Terim / Regex <span style={{ color: "#dc2626" }}>*</span>
                </label>
                <input
                  value={newTerm}
                  onChange={(e) => setNewTerm(e.target.value)}
                  placeholder="Örn: küfür veya /\d{10,11}/"
                  style={{
                    width: "100%",
                    padding: "8px 10px",
                    border: "1px solid #d1d5db",
                    borderRadius: 6,
                    boxSizing: "border-box",
                    fontFamily: newIsRegex ? "monospace" : "inherit",
                  }}
                />
              </div>
              <div>
                <label
                  style={{
                    fontSize: 12,
                    fontWeight: 600,
                    display: "block",
                    marginBottom: 4,
                  }}
                >
                  Kategori
                </label>
                <select
                  value={newCategory}
                  onChange={(e) =>
                    setNewCategory(e.target.value as BannedTerm["category"])
                  }
                  style={{
                    width: "100%",
                    padding: "8px 10px",
                    border: "1px solid #d1d5db",
                    borderRadius: 6,
                    background: "white",
                  }}
                >
                  {Object.entries(CATEGORY_LABELS).map(([k, v]) => (
                    <option key={k} value={k}>
                      {v.label}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label
                  style={{
                    fontSize: 12,
                    fontWeight: 600,
                    display: "block",
                    marginBottom: 4,
                  }}
                >
                  Risk Seviyesi
                </label>
                <select
                  value={newRisk}
                  onChange={(e) =>
                    setNewRisk(parseInt(e.target.value) as 1 | 2 | 3 | 4)
                  }
                  style={{
                    width: "100%",
                    padding: "8px 10px",
                    border: "1px solid #d1d5db",
                    borderRadius: 6,
                    background: "white",
                  }}
                >
                  {Object.entries(RISK_LABELS).map(([k, v]) => (
                    <option key={k} value={k}>
                      {k} — {v.label}
                    </option>
                  ))}
                </select>
              </div>
            </div>
            <label
              style={{
                display: "flex",
                alignItems: "center",
                gap: 8,
                fontSize: 13,
                cursor: "pointer",
                marginBottom: 16,
              }}
            >
              <input
                type="checkbox"
                checked={newIsRegex}
                onChange={(e) => setNewIsRegex(e.target.checked)}
              />
              Bu bir Regex ifadesidir
            </label>
            <div style={{ display: "flex", gap: 8 }}>
              <button
                onClick={() => addMutation.mutate()}
                disabled={addMutation.isPending || !newTerm.trim()}
                style={{
                  padding: "8px 16px",
                  backgroundColor: "#dc2626",
                  color: "white",
                  border: "none",
                  borderRadius: 6,
                  cursor:
                    addMutation.isPending || !newTerm.trim()
                      ? "not-allowed"
                      : "pointer",
                  fontWeight: 600,
                  opacity:
                    addMutation.isPending || !newTerm.trim() ? 0.6 : 1,
                }}
              >
                {addMutation.isPending ? "Ekleniyor..." : "Ekle"}
              </button>
              <button
                onClick={() => setShowAdd(false)}
                style={{
                  padding: "8px 16px",
                  border: "1px solid #d1d5db",
                  borderRadius: 6,
                  background: "white",
                  cursor: "pointer",
                }}
              >
                İptal
              </button>
            </div>
          </div>
        )}

        <div
          style={{ display: "flex", gap: 12, marginBottom: 16, flexWrap: "wrap" }}
        >
          <input
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Terim ara..."
            style={{
              flex: 1,
              minWidth: 180,
              padding: "8px 12px",
              border: "1px solid #d1d5db",
              borderRadius: 8,
              fontSize: 14,
            }}
          />
          <select
            value={filterCategory}
            onChange={(e) => setFilterCategory(e.target.value)}
            style={{
              padding: "8px 12px",
              border: "1px solid #d1d5db",
              borderRadius: 8,
              background: "white",
              fontSize: 14,
            }}
          >
            <option value="all">Tüm Kategoriler</option>
            {Object.entries(CATEGORY_LABELS).map(([k, v]) => (
              <option key={k} value={k}>
                {v.label}
              </option>
            ))}
          </select>
        </div>

        {isLoading ? (
          <div style={{ textAlign: "center", padding: 48, color: "#6b7280" }}>
            Yükleniyor...
          </div>
        ) : filteredTerms.length === 0 ? (
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
            {terms.length === 0
              ? "Henüz yasaklı terim eklenmemiş."
              : "Arama kriterlerine uyan terim bulunamadı."}
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
            <table style={{ width: "100%", borderCollapse: "collapse" }}>
              <thead>
                <tr
                  style={{
                    backgroundColor: "#f9fafb",
                    borderBottom: "1px solid #e5e7eb",
                  }}
                >
                  {["Terim", "Kategori", "Risk", "Tür", "İşlem"].map((h) => (
                    <th
                      key={h}
                      style={{
                        padding: "10px 16px",
                        textAlign: "left",
                        fontSize: 11,
                        fontWeight: 700,
                        color: "#6b7280",
                        textTransform: "uppercase",
                        letterSpacing: "0.05em",
                      }}
                    >
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {filteredTerms.map((t, idx) => {
                  const catInfo =
                    CATEGORY_LABELS[t.category] ?? CATEGORY_LABELS.custom;
                  const riskInfo = RISK_LABELS[t.riskLevel] ?? RISK_LABELS[2];

                  return (
                    <tr
                      key={t.id}
                      style={{
                        borderBottom:
                          idx < filteredTerms.length - 1
                            ? "1px solid #e5e7eb"
                            : "none",
                      }}
                    >
                      <td style={{ padding: "10px 16px" }}>
                        <code
                          style={{
                            fontFamily: "monospace",
                            fontSize: 13,
                            backgroundColor: "#f3f4f6",
                            padding: "2px 6px",
                            borderRadius: 4,
                          }}
                        >
                          {t.term}
                        </code>
                      </td>
                      <td style={{ padding: "10px 16px" }}>
                        <span
                          style={{
                            fontSize: 12,
                            color: catInfo.color,
                            fontWeight: 600,
                          }}
                        >
                          {catInfo.label}
                        </span>
                      </td>
                      <td style={{ padding: "10px 16px" }}>
                        <span
                          style={{
                            fontSize: 12,
                            color: riskInfo.color,
                            backgroundColor: riskInfo.bg,
                            padding: "2px 8px",
                            borderRadius: 10,
                            fontWeight: 600,
                          }}
                        >
                          {t.riskLevel} — {riskInfo.label}
                        </span>
                      </td>
                      <td style={{ padding: "10px 16px", fontSize: 12, color: "#6b7280" }}>
                        {t.isRegex ? "Regex" : "Düz Metin"}
                      </td>
                      <td style={{ padding: "10px 16px" }}>
                        <button
                          onClick={() => {
                            if (
                              confirm(
                                `"${t.term}" terimini kaldırmak istediğinizden emin misiniz?`
                              )
                            ) {
                              deleteMutation.mutate(t.id);
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
                          Kaldır
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        <div
          style={{
            marginTop: 20,
            backgroundColor: "#fffbeb",
            border: "1px solid #fde68a",
            borderRadius: 8,
            padding: 12,
            fontSize: 13,
            color: "#92400e",
          }}
        >
          <strong>Not:</strong> Risk seviyeleri — 1: Düşük (loglama), 2: Orta
          (uyarı), 3: Yüksek (moderasyon kuyruğu), 4: Kritik (otomatik ret).
          Regex ifadeleri sunucu taraflı Cloud Functions tarafından değerlendirilir.
        </div>
      </div>
    </AdminLayout>
  );
}