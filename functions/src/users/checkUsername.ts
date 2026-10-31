import * as admin from "firebase-admin";
import * as functions from "firebase-functions";

const db = admin.firestore();

/**
 * Kullanıcı adının müsait olup olmadığını kontrol eder.
 *
 * CF-AUTH-002: docs/14-cloud-functions-architecture.md
 * CLAUDE.md §5: Kullanıcı adı kayıt sırasında anlık kontrol edilmelidir.
 *
 * Kurallar:
 * - 3–20 karakter arası
 * - Yalnızca a-z, 0-9, alt çizgi (_) ve nokta (.) içerebilir
 * - Nokta veya alt çizgi ile başlayamaz/bitemez
 * - Büyük-küçük harf duyarsız (harfler küçüğe indirgenerek saklanır)
 * - users/usernames koleksiyonunda unique olmalıdır
 */
export const checkUsername = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
    const { username } = data;

    if (!username || typeof username !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Geçerli bir kullanıcı adı gereklidir."
      );
    }

    const normalized = username.toLowerCase().trim();

    // Format doğrulama
    const usernameRegex = /^[a-z0-9][a-z0-9._]{1,18}[a-z0-9]$|^[a-z0-9]{3,20}$/;
    if (!usernameRegex.test(normalized)) {
      return {
        available: false,
        reason: "Kullanıcı adı 3–20 karakter olmalı; harf, rakam, nokta ve alt çizgi içerebilir.",
      };
    }

    // Yasaklı kelime kontrolü
    const reservedNames = ["admin", "moderator", "mulkora", "support", "help"];
    if (reservedNames.some((r) => normalized.includes(r))) {
      return {
        available: false,
        reason: "Bu kullanıcı adı kullanılamaz.",
      };
    }

    // Veritabanı benzersizlik kontrolü
    const snap = await db.collection("usernames").doc(normalized).get();
    if (snap.exists) {
      return {
        available: false,
        reason: "Bu kullanıcı adı zaten kullanımda.",
      };
    }

    return { available: true };
  });