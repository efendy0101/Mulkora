/**
 * Admin Panel Kimlik Doğrulama Yardımcıları
 *
 * CLAUDE.md §21: Yönetici ve moderatör yetkileri Firebase custom claims ile doğrulanır.
 * Yalnızca role='admin' veya role='moderator' claim'ine sahip kullanıcılar erişebilir.
 */

import {
  GoogleAuthProvider,
  signInWithPopup,
  signOut as firebaseSignOut,
  onAuthStateChanged,
  type User,
} from "firebase/auth";
import { auth } from "./firebase";

export type AdminRole = "admin" | "moderator";

export interface AdminUser {
  uid: string;
  email: string | null;
  displayName: string | null;
  photoURL: string | null;
  role: AdminRole;
}

/**
 * Google ile giriş yapar ve custom claim'leri kontrol eder.
 * Yetkisiz kullanıcılar oturumu kapatılır ve hata fırlatılır.
 */
export async function signInWithGoogle(): Promise<AdminUser> {
  const provider = new GoogleAuthProvider();
  const result = await signInWithPopup(auth, provider);
  const user = result.user;

  const token = await user.getIdTokenResult();
  const role = token.claims["role"] as string | undefined;

  if (role !== "admin" && role !== "moderator") {
    await firebaseSignOut(auth);
    throw new Error("Yetkisiz erişim. Yalnızca yönetici ve moderatörler bu panele erişebilir.");
  }

  return {
    uid: user.uid,
    email: user.email,
    displayName: user.displayName,
    photoURL: user.photoURL,
    role: role as AdminRole,
  };
}

export async function signOut(): Promise<void> {
  await firebaseSignOut(auth);
}

/**
 * Mevcut kullanıcının admin/moderatör claim'ini doğrular.
 * Token'ı zorla yeniler (revoke durumunu yakalar).
 */
export async function verifyAdminClaim(user: User): Promise<AdminUser | null> {
  try {
    // forceRefresh: sunucu tarafında iptal edilen claim'leri yakala
    const token = await user.getIdTokenResult(true);
    const role = token.claims["role"] as string | undefined;

    if (role !== "admin" && role !== "moderator") {
      return null;
    }

    return {
      uid: user.uid,
      email: user.email,
      displayName: user.displayName,
      photoURL: user.photoURL,
      role: role as AdminRole,
    };
  } catch {
    return null;
  }
}

/**
 * Auth durumu değişikliklerini dinler.
 */
export function onAdminAuthStateChanged(
  callback: (user: AdminUser | null) => void
): () => void {
  return onAuthStateChanged(auth, async (firebaseUser) => {
    if (!firebaseUser) {
      callback(null);
      return;
    }
    const adminUser = await verifyAdminClaim(firebaseUser);
    callback(adminUser);
  });
}