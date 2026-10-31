"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { signInWithGoogle } from "@/lib/auth";
import toast from "react-hot-toast";

export default function LoginPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(false);

  async function handleGoogleSignIn() {
    setLoading(true);
    try {
      await signInWithGoogle();
      router.replace("/dashboard");
    } catch (err) {
      const message = err instanceof Error ? err.message : "Giriş başarısız";
      toast.error(message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div
      style={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        background: "var(--color-bg)",
      }}
    >
      <div className="card" style={{ maxWidth: 380, width: "100%", textAlign: "center" }}>
        <h1 style={{ fontSize: "1.5rem", fontWeight: 700, marginBottom: 8 }}>
          Mülkora
        </h1>
        <p style={{ color: "var(--color-text-secondary)", marginBottom: 32, fontSize: "0.875rem" }}>
          Yönetim Paneli
        </p>
        <p style={{ marginBottom: 24, fontSize: "0.875rem", color: "var(--color-text-secondary)" }}>
          Yalnızca yönetici ve moderatörler giriş yapabilir.
        </p>
        <button
          className="btn btn-primary"
          style={{ width: "100%", justifyContent: "center", padding: "12px 16px" }}
          onClick={handleGoogleSignIn}
          disabled={loading}
        >
          {loading ? (
            <div className="spinner" style={{ width: 20, height: 20, borderWidth: 2 }} />
          ) : (
            "Google ile Giriş Yap"
          )}
        </button>
      </div>
    </div>
  );
}