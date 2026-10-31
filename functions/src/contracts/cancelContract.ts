import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "../firebaseAdmin";

const db = admin.firestore();

/**
 * Sözleşme taslağını iptal eder.
 *
 * Yalnızca DRAFT veya AWAITING_PARTY2 durumundaki sözleşmeler iptal edilebilir.
 * Sadece party1 veya party2 bu işlemi gerçekleştirebilir.
 *
 * docs/14-cloud-functions-architecture.md — CF-CONTRACT-007
 * CLAUDE.md §26: Sözleşme işlemleri yalnızca taraflar tarafından yapılabilir.
 */
export const cancelContract = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError("unauthenticated", "Kimlik doğrulama gereklidir.");
  }

  const { contractId } = request.data as { contractId?: string };
  if (!contractId || typeof contractId !== "string") {
    throw new HttpsError("invalid-argument", "contractId zorunludur.");
  }

  const contractRef = db.collection("contracts").doc(contractId);

  return db.runTransaction(async (tx) => {
    const snap = await tx.get(contractRef);
    if (!snap.exists) {
      throw new HttpsError("not-found", "Sözleşme bulunamadı.");
    }

    const data = snap.data()!;

    // Yetki kontrolü: sadece taraflar iptal edebilir
    if (data.party1Uid !== uid && data.party2Uid !== uid) {
      throw new HttpsError(
        "permission-denied",
        "Bu sözleşmeyi iptal etme yetkiniz yok."
      );
    }

    // Durum kontrolü: yalnızca DRAFT veya AWAITING_PARTY2
    const allowedStatuses = ["draft", "awaiting_party2"];
    if (!allowedStatuses.includes(data.status)) {
      throw new HttpsError(
        "failed-precondition",
        `Sözleşme '${data.status}' durumunda olduğu için iptal edilemez. ` +
          "Yalnızca taslak veya davet bekleyen sözleşmeler iptal edilebilir."
      );
    }

    tx.update(contractRef, {
      status: "cancelled",
      cancelledBy: uid,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    return { success: true };
  });
});