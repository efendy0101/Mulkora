import * as admin from "../firebaseAdmin";
import * as functions from "firebase-functions/v1";

const db = admin.firestore();

/**
 * Kullanıcı engelini kaldırır.
 * CLAUDE.md Bölüm 3: Kullanıcı Yönetimi
 *
 * blockUser.ts ile simetrik olarak çalışır.
 * blockedUsers/{blockerUid}_{blockedUid} belgesi silinir.
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

    if (uid === targetUid) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Kendinizin engelini kaldıramazsınız."
      );
    }

    const blockId = `${uid}_${targetUid}`;
    const blockRef = db.collection("blockedUsers").doc(blockId);
    const blockSnap = await blockRef.get();

    if (!blockSnap.exists) {
      throw new functions.https.HttpsError(
        "not-found",
        "Bu kullanıcı zaten engellenmemiş."
      );
    }

    await blockRef.delete();

    return { success: true };
  });