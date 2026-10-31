import * as admin from "../firebaseAdmin";
import * as functions from "firebase-functions/v1";

const db = admin.firestore();

/**
 * Kullanıcı profil bilgilerini günceller.
 *
 * CF-USER-003: docs/14-cloud-functions-architecture.md
 * CLAUDE.md §5: Profil güncellemesi güvenli şekilde Cloud Functions üzerinden yapılır.
 *
 * Güncellenebilir alanlar:
 * - displayName (1–50 karakter)
 * - bio (max 300 karakter)
 * - city (max 50 karakter)
 * - photoUrl (Firebase Storage URL'si olmalı)
 *
 * username ve role değiştirilemez bu endpoint üzerinden.
 */
export const updateProfile = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Bu işlem için giriş yapılmış olmalıdır."
      );
    }

    const uid = context.auth.uid;
    const { displayName, bio, city, photoUrl } = data;

    // Güncellenecek alan en az bir tane olmalı
    if (
      displayName === undefined &&
      bio === undefined &&
      city === undefined &&
      photoUrl === undefined
    ) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Güncellenecek en az bir alan gereklidir."
      );
    }

    const updateData: Record<string, unknown> = {
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    };

    if (displayName !== undefined) {
      if (
        typeof displayName !== "string" ||
        displayName.trim().length < 1 ||
        displayName.trim().length > 50
      ) {
        throw new functions.https.HttpsError(
          "invalid-argument",
          "Görünen ad 1–50 karakter arasında olmalıdır."
        );
      }
      updateData.displayName = displayName.trim();
    }

    if (bio !== undefined) {
      if (typeof bio !== "string" || bio.length > 300) {
        throw new functions.https.HttpsError(
          "invalid-argument",
          "Bio maksimum 300 karakter olabilir."
        );
      }
      updateData.bio = bio.trim();
    }

    if (city !== undefined) {
      if (typeof city !== "string" || city.length > 50) {
        throw new functions.https.HttpsError(
          "invalid-argument",
          "Şehir adı maksimum 50 karakter olabilir."
        );
      }
      updateData.city = city.trim();
    }

    if (photoUrl !== undefined) {
      if (typeof photoUrl !== "string") {
        throw new functions.https.HttpsError(
          "invalid-argument",
          "Geçerli bir fotoğraf URL'si gereklidir."
        );
      }
      // Firebase Storage URL'si kontrolü
      const validStorageDomain =
        photoUrl.startsWith("https://firebasestorage.googleapis.com") ||
        photoUrl.startsWith("https://storage.googleapis.com");
      if (!validStorageDomain) {
        throw new functions.https.HttpsError(
          "invalid-argument",
          "Fotoğraf yalnızca Firebase Storage'dan yüklenebilir."
        );
      }
      updateData.photoUrl = photoUrl;
    }

    await db.collection("users").doc(uid).update(updateData);

    functions.logger.info(`Profil güncellendi: ${uid}`, { fields: Object.keys(updateData) });

    return { success: true };
  });