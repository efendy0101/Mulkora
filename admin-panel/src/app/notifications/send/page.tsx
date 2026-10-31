"use client";

/**
 * SCR-ADMIN-018: Bildirim Gönderimi Sayfası
 *
 * Yöneticilerin tüm kullanıcılara veya belirli segmentlere
 * sistem duyurusu push bildirimi gönderebileceği panel sayfası.
 *
 * CLAUDE.md §16 ve §3.5: Yönetici duyurusu bildirimi — admin only
 * docs/19-notification-system.md
 * Cloud Function: onNotificationCreated (FCM topik bazlı gönderim)
 */

import { useState } from "react";
import { getFunctions, httpsCallable } from "firebase/functions";
import { useAdminAuth } from "@/hooks/useAdminAuth";
import { AdminLayout } from "@/components/AdminLayout";
import toast from "react-hot-toast";

type RecipientType = "all" | "tenants" | "landlords" | "all_tenants_and_landlords";

const RECIPIENT_OPTIONS: { value: RecipientType; label: string; description: string }[] = [
  {
    value: "all",
    label: "Tüm Kullanıcılar",
    description: "Uygulamayı kullanan herkese gönderilir",
  },
  {
    value: "tenants",
    label: "Yalnızca Kiracılar",
    description: "Kiracı rolüne sahip kullanıcılara gönderilir",
  },
  {
    value: "landlords",
    label: "Yalnızca Ev Sahipleri",
    description: "Ev sahibi rolüne sahip kullanıcılara gönderilir",
  },
  {
    value: "all_tenants_and_landlords",
    label: "Kiracı ve Ev Sahipleri",
    description: "Her iki role sahip kullanıcılara gönderilir",
  },
];

export default function SendNotificationPage() {
  const { user } = useAdminAuth();
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [recipient, setRecipient] = useState<RecipientType>("all");
  const [deepLink, setDeepLink] = useState("");
  const [isSending, setIsSending] = useState(false);
  const [sentCount, setSentCount] = useState<number | null>(null);
  const functions = getFunctions(undefined, "europe-west1");

  // Sadece admin erişebilir
  if (user && user.role !== "admin") {
    return (
      <AdminLayout>
        <div
          style={{
            maxWidth: 480,
            margin: "80px auto",
            textAlign: "center",
            color: "#dc2626",
          }}
        >
          <div style={{ fontSize: 48, marginBottom: 16 }}>🔒</div>
          <h2>Erişim Reddedildi</h2>
          <p style={{ color: "#6b7280" }}>Bu sayfaya yalnızca yöneticiler erişebilir.</p>
        </div>
      </AdminLayout>
    );
  }

  const handleSend = async () => {
    if (!title.trim()) {
      toast.error("Bildirim başlığı zorunludur.");
      return;
    }
    if (!body.trim()) {
      toast.error("Bildirim metni zorunludur.");
      return;
    }
    if (!confirm(`"${title}" başlıklı bildirim ${RECIPIENT_OPTIONS.find(r => r.value === recipient)?.label} grubuna gönderilecek. Onaylıyor musunuz?`)) return;

    setIsSending(true);
    setSentCount(null);
    try {
      const sendAnnouncement = httpsCallable(functions, "sendSystemAnnouncement");
      const result = await sendAnnouncement({
        title: title.trim(),
        body: body.trim(),
        recipient,
        deepLink: deepLink.trim() || null,
      });
      const data = result.data as { sentCount?: number };
      setSentCount(data?.sentCount ?? 0);
      toast.success("Bildirim başarıyla gönderildi.");
      setTitle("");
      setBody("");
      setDeepLink("");
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : "Bilinmeyen hata";
      toast.error(`Hata: ${message}`);
    } finally {
      setIsSending(false);
    }
  };

  const characterLimit = 200;
  const remaining = characterLimit - body.length;

  return (
    <AdminLayout>
      <div style={{ maxWidth: 680, margin: "0 auto" }}>
        <div style={{ marginBottom: 28 }}>
          <h1 style={{ margin: 0, fontSize: 24 }}>Bildirim Gönder</h1>
          <p style={{ margin: "4px 0 0", color: "#6b7280" }}>
            Kullanıcılara sistem duyurusu push bildirimi gönderin.
          </p>
        </div>

        {/* Gönderim Formu */}
        <div
          style={{
            backgroundColor: "white",
            border: "1px solid #e5e7eb",
            borderRadius: 12,
            padding: 24,
            marginBottom: 20,
          }}
        >
          {/* Alıcı Seçimi */}
          <div style={{ marginBottom: 20 }}>
            <label
              style={{
                display: "block",
                fontSize: 14,
                fontWeight: 600,
                color: "#374151",
                marginBottom: 8,
              }}
            >
              Alıcılar
            </label>
            <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
              {RECIPIENT_OPTIONS.map((opt) => (
                <label
                  key={opt.value}
                  style={{
                    display: "flex",
                    alignItems: "flex-start",
                    gap: 10,
                    padding: 12,
                    border: `1px solid ${recipient === opt.value ? "#2563eb" : "#e5e7eb"}`,
                    borderRadius: 8,
                    cursor: "pointer",
                    backgroundColor: recipient === opt.value ? "#eff6ff" : "white",
                  }}
                >
                  <input
                    type="radio"
                    name="recipient"
                    value={opt.value}
                    checked={recipient === opt.value}
                    onChange={() => setRecipient(opt.value)}
                    style={{ marginTop: 2 }}
                  />
                  <div>
                    <div style={{ fontSize: 14, fontWeight: 600, color: "#111827" }}>
                      {opt.label}
                    </div>
                    <div style={{ fontSize: 12, color: "#6b7280", marginTop: 1 }}>
                      {opt.description}
                    </div>
                  </div>
                </label>
              ))}
            </div>
          </div>

          {/* Başlık */}
          <div style={{ marginBottom: 16 }}>
            <label
              style={{
                display: "block",
                fontSize: 14,
                fontWeight: 600,
                color: "#374151",
                marginBottom: 6,
              }}
            >
              Başlık <span style={{ color: "#dc2626" }}>*</span>
            </label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Bildirim başlığı..."
              maxLength={100}
              style={{
                width: "100%",
                padding: "8px 12px",
                borderRadius: 8,
                border: "1px solid #d1d5db",
                fontSize: 14,
                boxSizing: "border-box",
              }}
            />
            <div style={{ fontSize: 11, color: "#9ca3af", marginTop: 3, textAlign: "right" }}>
              {title.length}/100
            </div>
          </div>

          {/* Mesaj */}
          <div style={{ marginBottom: 16 }}>
            <label
              style={{
                display: "block",
                fontSize: 14,
                fontWeight: 600,
                color: "#374151",
                marginBottom: 6,
              }}
            >
              Mesaj <span style={{ color: "#dc2626" }}>*</span>
            </label>
            <textarea
              value={body}
              onChange={(e) => setBody(e.target.value.slice(0, characterLimit))}
              placeholder="Bildirim metni..."
              rows={4}
              style={{
                width: "100%",
                padding: "8px 12px",
                borderRadius: 8,
                border: `1px solid ${remaining < 20 ? "#f97316" : "#d1d5db"}`,
                fontSize: 14,
                resize: "vertical",
                boxSizing: "border-box",
              }}
            />
            <div
              style={{
                fontSize: 11,
                color: remaining < 20 ? "#f97316" : "#9ca3af",
                marginTop: 3,
                textAlign: "right",
              }}
            >
              {remaining} karakter kaldı
            </div>
          </div>

          {/* Derin Bağlantı (Opsiyonel) */}
          <div style={{ marginBottom: 20 }}>
            <label
              style={{
                display: "block",
                fontSize: 14,
                fontWeight: 600,
                color: "#374151",
                marginBottom: 6,
              }}
            >
              Derin Bağlantı (Opsiyonel)
            </label>
            <input
              type="text"
              value={deepLink}
              onChange={(e) => setDeepLink(e.target.value)}
              placeholder="mulkora://posts/abc123"
              style={{
                width: "100%",
                padding: "8px 12px",
                borderRadius: 8,
                border: "1px solid #d1d5db",
                fontSize: 14,
                boxSizing: "border-box",
              }}
            />
            <p style={{ margin: "4px 0 0", fontSize: 11, color: "#9ca3af" }}>
              Bildirime tıklandığında açılacak uygulama içi sayfa
            </p>
          </div>

          <button
            onClick={handleSend}
            disabled={isSending || !title.trim() || !body.trim()}
            style={{
              width: "100%",
              padding: "12px",
              backgroundColor:
                isSending || !title.trim() || !body.trim() ? "#9ca3af" : "#2563eb",
              color: "white",
              border: "none",
              borderRadius: 8,
              cursor: isSending || !title.trim() || !body.trim() ? "not-allowed" : "pointer",
              fontWeight: 600,
              fontSize: 15,
            }}
          >
            {isSending ? "Gönderiliyor..." : "Bildirimi Gönder"}
          </button>
        </div>

        {/* Başarı Mesajı */}
        {sentCount !== null && (
          <div
            style={{
              backgroundColor: "#f0fdf4",
              border: "1px solid #bbf7d0",
              borderRadius: 12,
              padding: 20,
              textAlign: "center",
              color: "#15803d",
            }}
          >
            <div style={{ fontSize: 32, marginBottom: 8 }}>✓</div>
            <div style={{ fontWeight: 600, fontSize: 16 }}>Bildirim Gönderildi!</div>
            <div style={{ marginTop: 4, color: "#6b7280", fontSize: 13 }}>
              Tahminen {sentCount} kullanıcıya iletildi.
            </div>
          </div>
        )}

        {/* Bilgi Notu */}
        <div
          style={{
            backgroundColor: "#fffbeb",
            border: "1px solid #fde68a",
            borderRadius: 10,
            padding: 14,
            marginTop: 16,
            fontSize: 13,
            color: "#92400e",
          }}
        >
          <strong>⚠️ Önemli:</strong> Push bildirimler geri alınamaz. Göndermeden önce içeriği
          dikkatlice kontrol edin. Bildirimler yalnızca bildirim izni açık olan kullanıcılara
          iletilir.
        </div>
      </div>
    </AdminLayout>
  );
}