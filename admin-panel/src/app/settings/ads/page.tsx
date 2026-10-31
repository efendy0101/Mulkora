"use client";

/**
 * SCR-ADMIN-017: Reklam Ayarları Sayfası
 *
 * Yöneticilerin AdMob reklam davranışını, sıklık sınırlarını ve
 * reklam durumunu Firebase Remote Config / adSettings koleksiyonu
 * üzerinden yönetebileceği panel sayfası.
 *
 * CLAUDE.md §17 ve §3.5: Reklam ayarları — admin only
 * docs/20-ads-billing-design.md
 */

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { doc, getDoc, setDoc, serverTimestamp } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAdminAuth } from "@/hooks/useAdminAuth";
import { AdminLayout } from "@/components/AdminLayout";
import toast from "react-hot-toast";

interface AdSettings {
  adsEnabled: boolean;
  nativeAdFrequency: number;         // Her kaç gönderide bir native reklam (min 5)
  interstitialCooldownMinutes: number; // Aynı kullanıcıya interstitial arası minimum süre (dk)
  maxDailyInterstitials: number;       // Günlük maksimum tam ekran reklam
  initialPostsBeforeAd: number;        // İlk X gönderide reklam gösterme
  hideAdsOnWriting: boolean;           // Kullanıcı yazarken reklam gizle
  updatedAt?: { toDate: () => Date };
  updatedBy?: string;
}

const DEFAULT_SETTINGS: Omit<AdSettings, "updatedAt" | "updatedBy"> = {
  adsEnabled: true,
  nativeAdFrequency: 8,
  interstitialCooldownMinutes: 10,
  maxDailyInterstitials: 3,
  initialPostsBeforeAd: 4,
  hideAdsOnWriting: true,
};

export default function AdSettingsPage() {
  const { user } = useAdminAuth();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<Omit<AdSettings, "updatedAt" | "updatedBy">>(DEFAULT_SETTINGS);
  const [isDirty, setIsDirty] = useState(false);


  const { isLoading } = useQuery({
    queryKey: ["ad-settings"],
    queryFn: async () => {
      const docRef = doc(db, "adSettings", "global");
      const snap = await getDoc(docRef);
      if (snap.exists()) {
        const data = snap.data() as AdSettings;
        setForm({
          adsEnabled: data.adsEnabled ?? DEFAULT_SETTINGS.adsEnabled,
          nativeAdFrequency: data.nativeAdFrequency ?? DEFAULT_SETTINGS.nativeAdFrequency,
          interstitialCooldownMinutes:
            data.interstitialCooldownMinutes ?? DEFAULT_SETTINGS.interstitialCooldownMinutes,
          maxDailyInterstitials:
            data.maxDailyInterstitials ?? DEFAULT_SETTINGS.maxDailyInterstitials,
          initialPostsBeforeAd:
            data.initialPostsBeforeAd ?? DEFAULT_SETTINGS.initialPostsBeforeAd,
          hideAdsOnWriting: data.hideAdsOnWriting ?? DEFAULT_SETTINGS.hideAdsOnWriting,
        });
        return data;
      }
      return null;
    },
    enabled: user?.role === "admin",
  });

  const saveMutation = useMutation({
    mutationFn: async () => {
      const docRef = doc(db, "adSettings", "global");
      await setDoc(
        docRef,
        {
          ...form,
          updatedAt: serverTimestamp(),
          updatedBy: user?.uid,
        },
        { merge: true }
      );
    },
    onSuccess: () => {
      toast.success("Reklam ayarları kaydedildi.");
      setIsDirty(false);
      queryClient.invalidateQueries({ queryKey: ["ad-settings"] });
    },
    onError: (error: unknown) => {
      const message = error instanceof Error ? error.message : "Bilinmeyen hata";
      toast.error(`Hata: ${message}`);
    },
  });

  // Sadece admin erişebilir
  if (user && user.role !== "admin") {
    return (
      <AdminLayout>
        <div style={{ maxWidth: 480, margin: "80px auto", textAlign: "center", color: "#dc2626" }}>
          <div style={{ fontSize: 48, marginBottom: 16 }}>🔒</div>
          <h2>Erişim Reddedildi</h2>
          <p style={{ color: "#6b7280" }}>Bu sayfaya yalnızca yöneticiler erişebilir.</p>
        </div>
      </AdminLayout>
    );
  }


  function updateField<K extends keyof typeof form>(key: K, value: (typeof form)[K]) {
    setForm((prev) => ({ ...prev, [key]: value }));
    setIsDirty(true);
  }

  return (
    <AdminLayout>
      <div style={{ maxWidth: 680, margin: "0 auto" }}>
        <div style={{ marginBottom: 28 }}>
          <h1 style={{ margin: 0, fontSize: 24 }}>Reklam Ayarları</h1>
          <p style={{ margin: "4px 0 0", color: "#6b7280" }}>
            AdMob reklam davranışını ve sıklık sınırlarını yönetin.
          </p>
        </div>

        {isLoading ? (
          <div style={{ textAlign: "center", padding: 48, color: "#6b7280" }}>Yükleniyor...</div>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
            {/* Reklamları Aç/Kapat */}
            <div
              style={{
                backgroundColor: "white",
                border: "1px solid #e5e7eb",
                borderRadius: 12,
                padding: 20,
              }}
            >
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                }}
              >
                <div>
                  <div style={{ fontWeight: 600, fontSize: 15 }}>Reklamları Etkinleştir</div>
                  <div style={{ fontSize: 13, color: "#6b7280", marginTop: 2 }}>
                    Kapatıldığında ücretli üyeliği olmayan kullanıcılara da reklam gösterilmez.
                    Bakım veya acil durumlar için kullanın.
                  </div>
                </div>
                <label style={{ position: "relative", display: "inline-block", width: 48, height: 26 }}>
                  <input
                    type="checkbox"
                    checked={form.adsEnabled}
                    onChange={(e) => updateField("adsEnabled", e.target.checked)}
                    style={{ opacity: 0, width: 0, height: 0 }}
                  />
                  <span
                    style={{
                      position: "absolute",
                      cursor: "pointer",
                      top: 0,
                      left: 0,
                      right: 0,
                      bottom: 0,
                      backgroundColor: form.adsEnabled ? "#2563eb" : "#d1d5db",
                      borderRadius: 13,
                      transition: "0.3s",
                    }}
                  >
                    <span
                      style={{
                        position: "absolute",
                        height: 20,
                        width: 20,
                        left: form.adsEnabled ? 24 : 3,
                        bottom: 3,
                        backgroundColor: "white",
                        borderRadius: "50%",
                        transition: "0.3s",
                      }}
                    />
                  </span>
                </label>
              </div>
            </div>

            {/* Native Reklam Sıklığı */}
            <div
              style={{
                backgroundColor: "white",
                border: "1px solid #e5e7eb",
                borderRadius: 12,
                padding: 20,
              }}
            >
              <label style={{ display: "block", fontWeight: 600, fontSize: 15, marginBottom: 4 }}>
                Native Reklam Sıklığı
              </label>
              <p style={{ margin: "0 0 12px", fontSize: 13, color: "#6b7280" }}>
                Ana akışta her kaç gönderide bir native reklam gösterilsin? (minimum: 5)
              </p>
              <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                <input
                  type="range"
                  min={5}
                  max={20}
                  value={form.nativeAdFrequency}
                  onChange={(e) => updateField("nativeAdFrequency", Number(e.target.value))}
                  style={{ flex: 1 }}
                />
                <span
                  style={{
                    minWidth: 48,
                    textAlign: "center",
                    fontWeight: 700,
                    fontSize: 18,
                    color: "#2563eb",
                  }}
                >
                  {form.nativeAdFrequency}
                </span>
              </div>
              <div style={{ display: "flex", justifyContent: "space-between", fontSize: 11, color: "#9ca3af", marginTop: 2 }}>
                <span>5 (sık)</span>
                <span>20 (seyrek)</span>
              </div>
            </div>

            {/* Interstitial Soğuma Süresi */}
            <div
              style={{
                backgroundColor: "white",
                border: "1px solid #e5e7eb",
                borderRadius: 12,
                padding: 20,
              }}
            >
              <label style={{ display: "block", fontWeight: 600, fontSize: 15, marginBottom: 4 }}>
                Interstitial Soğuma Süresi (dakika)
              </label>
              <p style={{ margin: "0 0 12px", fontSize: 13, color: "#6b7280" }}>
                Aynı kullanıcıya iki tam ekran reklam arasındaki minimum süre.
              </p>
              <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                <input
                  type="range"
                  min={5}
                  max={60}
                  step={5}
                  value={form.interstitialCooldownMinutes}
                  onChange={(e) => updateField("interstitialCooldownMinutes", Number(e.target.value))}
                  style={{ flex: 1 }}
                />
                <span
                  style={{
                    minWidth: 56,
                    textAlign: "center",
                    fontWeight: 700,
                    fontSize: 18,
                    color: "#2563eb",
                  }}
                >
                  {form.interstitialCooldownMinutes} dk
                </span>
              </div>
              <div style={{ display: "flex", justifyContent: "space-between", fontSize: 11, color: "#9ca3af", marginTop: 2 }}>
                <span>5 dk</span>
                <span>60 dk</span>
              </div>
            </div>

            {/* Günlük Maksimum Interstitial */}
            <div
              style={{
                backgroundColor: "white",
                border: "1px solid #e5e7eb",
                borderRadius: 12,
                padding: 20,
              }}
            >
              <label style={{ display: "block", fontWeight: 600, fontSize: 15, marginBottom: 4 }}>
                Günlük Maksimum Tam Ekran Reklam
              </label>
              <p style={{ margin: "0 0 12px", fontSize: 13, color: "#6b7280" }}>
                Bir kullanıcıya günde gösterilebilecek maksimum interstitial reklam sayısı.
              </p>
              <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                <input
                  type="range"
                  min={1}
                  max={10}
                  value={form.maxDailyInterstitials}
                  onChange={(e) => updateField("maxDailyInterstitials", Number(e.target.value))}
                  style={{ flex: 1 }}
                />
                <span
                  style={{
                    minWidth: 48,
                    textAlign: "center",
                    fontWeight: 700,
                    fontSize: 18,
                    color: "#2563eb",
                  }}
                >
                  {form.maxDailyInterstitials}
                </span>
              </div>
            </div>

            {/* İlk Gönderi Eşiği */}
            <div
              style={{
                backgroundColor: "white",
                border: "1px solid #e5e7eb",
                borderRadius: 12,
                padding: 20,
              }}
            >
              <label style={{ display: "block", fontWeight: 600, fontSize: 15, marginBottom: 4 }}>
                Başlangıç Tampon Gönderisi
              </label>
              <p style={{ margin: "0 0 12px", fontSize: 13, color: "#6b7280" }}>
                Ana akışın ilk kaç gönderisinde reklam gösterilmesin? (CLAUDE.md §17)
              </p>
              <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                <input
                  type="range"
                  min={2}
                  max={10}
                  value={form.initialPostsBeforeAd}
                  onChange={(e) => updateField("initialPostsBeforeAd", Number(e.target.value))}
                  style={{ flex: 1 }}
                />
                <span
                  style={{
                    minWidth: 56,
                    textAlign: "center",
                    fontWeight: 700,
                    fontSize: 18,
                    color: "#2563eb",
                  }}
                >
                  {form.initialPostsBeforeAd} gönderi
                </span>
              </div>
            </div>

            {/* Yazarken Gizle */}
            <div
              style={{
                backgroundColor: "white",
                border: "1px solid #e5e7eb",
                borderRadius: 12,
                padding: 20,
              }}
            >
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <div>
                  <div style={{ fontWeight: 600, fontSize: 15 }}>Yazarken Reklamları Gizle</div>
                  <div style={{ fontSize: 13, color: "#6b7280", marginTop: 2 }}>
                    Kullanıcı gönderi/yorum yazarken reklamlar gizlenir (CLAUDE.md §17).
                  </div>
                </div>
                <label style={{ position: "relative", display: "inline-block", width: 48, height: 26 }}>
                  <input
                    type="checkbox"
                    checked={form.hideAdsOnWriting}
                    onChange={(e) => updateField("hideAdsOnWriting", e.target.checked)}
                    style={{ opacity: 0, width: 0, height: 0 }}
                  />
                  <span
                    style={{
                      position: "absolute",
                      cursor: "pointer",
                      top: 0,
                      left: 0,
                      right: 0,
                      bottom: 0,
                      backgroundColor: form.hideAdsOnWriting ? "#2563eb" : "#d1d5db",
                      borderRadius: 13,
                      transition: "0.3s",
                    }}
                  >
                    <span
                      style={{
                        position: "absolute",
                        height: 20,
                        width: 20,
                        left: form.hideAdsOnWriting ? 24 : 3,
                        bottom: 3,
                        backgroundColor: "white",
                        borderRadius: "50%",
                        transition: "0.3s",
                      }}
                    />
                  </span>
                </label>
              </div>
            </div>

            {/* Kaydet Butonu */}
            <button
              onClick={() => saveMutation.mutate()}
              disabled={!isDirty || saveMutation.isPending}
              style={{
                padding: "12px",
                backgroundColor: !isDirty || saveMutation.isPending ? "#9ca3af" : "#2563eb",
                color: "white",
                border: "none",
                borderRadius: 8,
                cursor: !isDirty || saveMutation.isPending ? "not-allowed" : "pointer",
                fontWeight: 600,
                fontSize: 15,
              }}
            >
              {saveMutation.isPending ? "Kaydediliyor..." : isDirty ? "Değişiklikleri Kaydet" : "Kaydedildi"}
            </button>
          </div>
        )}
      </div>
    </AdminLayout>
  );
}