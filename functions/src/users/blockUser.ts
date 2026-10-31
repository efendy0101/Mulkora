import * as admin from "firebase-admin";
import * as functions from "firebase-functions";

const db = admin.firestore();

/**
 * Kullanıcıyı engeller.
 * CLAUDE.md Bölüm 3: Kullanıcı Yönetimi
 *
 * Engellenen kullanıcı:
 * - Bloke eden kullanıcının gönderilerini göremez
 * - Bloke eden kullanıcıya yorum/tepki yapamaz
 */
export const blockUser = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Bu işlem için giriş yapılmış olmalıdır."
      );
    }

    const uid = context.auth.uid;
    const { targetUid } = data;

    if (!targetUid || typeof targetUid !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Geçerli bir targetUid gereklidir."
      );
    }

    if (uid === targetUid) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Kendinizi engelleyemezsiniz."
      );
    }

    const targetSnap = await db.collection("users").doc(targetUid).get();
    if (!targetSnap.exists) {
      throw new functions.https.HttpsError("not-found", "Kullanıcı bulunamadı.");
    }

    const now = admin.firestore.FieldValue.serverTimestamp();

    // Engel kaydını oluştur
    await db
      .collection("users")
      .doc(uid)
      .collection("blockedUsers")
      .doc(targetUid)
      .set({
        blockedUid: targetUid,
        blockedAt: now,
      });

    functions.logger.info(`Kullanıcı engellendi: ${uid} → ${targetUid}`);

    return { success: true };
  });

/**
 * Kullanıcı engelini kaldırır.
 */
export const unblockUser = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Bu işlem için giriş yapılmış olmalıdır."
      );
    }

    const uid = context.auth.uid;
    const { targetUid } = data;

    if (!targetUid || typeof targetUid !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Geçerli bir targetUid gereklidir."
      );
    }

    await db
      .collection("users")
      .doc(uid)
      .collection("blockedUsers")
      .doc(targetUid)
      .delete();

    functions.logger.info(`Kullanıcı engeli kaldırıldı: ${uid} → ${targetUid}`);

    return { success: true };
  });