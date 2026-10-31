/**
 * Yönetim paneli Firebase istemci yapılandırması.
 *
 * CLAUDE.md §21: Yönetici ve moderatör yetkileri Firebase custom claims ile doğrulanır.
 * CLAUDE.md §35: API anahtarları bu dosyaya yazılmaz; .env.local üzerinden alınır.
 *
 * NOT: `output: "export"` (statik) modunda Next.js build sırasında tüm istemci bileşenleri
 *       pre-render edilir. Firebase SDK'sının `initializeApp()` çağrısı, geçerli bir apiKey
 *       olmadığında `auth/invalid-api-key` fırlatır. Bu nedenle `.env.local` mevcut değilken
 *       placeholder değerler kullanılır — gerçek ağ işlemleri yapmadıkça hata olmaz.
 *       Browser ortamında gerçek env değerleri devreye girer.
 *
 * .env.local (proje kökünde, git'e eklenmez):
 *   NEXT_PUBLIC_FIREBASE_API_KEY=...
 *   NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN=...
 *   NEXT_PUBLIC_FIREBASE_PROJECT_ID=...
 *   NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET=...
 *   NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID=...
 *   NEXT_PUBLIC_FIREBASE_APP_ID=...
 */

import { initializeApp, getApps } from "firebase/app";
import { getAuth, connectAuthEmulator } from "firebase/auth";
import { getFirestore, connectFirestoreEmulator } from "firebase/firestore";
import { getFunctions, connectFunctionsEmulator } from "firebase/functions";

const firebaseConfig = {
  // Placeholder değerler: build sırasında .env.local yoksa SSR pre-render başarısız olmaz.
  // Gerçek değerler browser'da NEXT_PUBLIC_ env vars'tan gelir.
  apiKey: process.env.NEXT_PUBLIC_FIREBASE_API_KEY || "placeholder-api-key",
  authDomain:
    process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN || "placeholder.firebaseapp.com",
  projectId: process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID || "placeholder-project",
  storageBucket:
    process.env.NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET || "placeholder.appspot.com",
  messagingSenderId:
    process.env.NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID || "000000000000",
  appId: process.env.NEXT_PUBLIC_FIREBASE_APP_ID || "1:000000:web:placeholder",
};

// Çoklu init koruması (Next.js HMR)
const app = getApps().length === 0 ? initializeApp(firebaseConfig) : getApps()[0];

export const auth = getAuth(app);
export const db = getFirestore(app);
export const functions = getFunctions(app, "europe-west1");

// Geliştirme ortamında Emulator Suite'e bağlan
if (
  typeof window !== "undefined" &&
  process.env.NODE_ENV === "development" &&
  process.env.NEXT_PUBLIC_USE_EMULATOR === "true"
) {
  connectAuthEmulator(auth, "http://127.0.0.1:9099", { disableWarnings: true });
  connectFirestoreEmulator(db, "127.0.0.1", 8080);
  connectFunctionsEmulator(functions, "127.0.0.1", 5001);
}