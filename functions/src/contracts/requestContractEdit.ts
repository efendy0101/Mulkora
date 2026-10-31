import * as admin from "../firebaseAdmin";
import * as functions from "firebase-functions/v1";

const db = admin.firestore();

/**
 * Sözleşme taraflarından biri düzenleme talebinde bulunur.
 * docs/22-development-phases.md Faz 9 — requestContractEdit
 *
 * Akış:
 * 1. Yalnızca sözleşmenin tarafları düzenleme talebinde bulunabilir
 * 2. Sözleşme kilitli olmamalı
 * 3. Durum "awaiting_approval" veya "edit_requested" ise tekrar talep edilebilir
 * 4. Hem party1 hem party2 onayı sıfırlanır — yeniden onay gerekir
 * 5. Düzenleme notu diğer tarafa bildirilir
 */
export const requestContractEdit = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Bu işlem için giriş yapılmış olmalıdır."
      );
    }

    const uid = context.auth.uid;
    const { contractId, editNote } = data;

    if (!contractId || typeof contractId !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Geçerli bir contractId gereklidir."
      );
    }

    if (!editNote || typeof editNote !== "string" || editNote.trim().length < 5) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Düzenleme notu en az 5 karakter olmalıdır."
      );
    }

    const contractDoc = db.collection("contracts").doc(contractId);
    const contractSnap = await contractDoc.get();

    if (!contractSnap.exists) {
      throw new functions.https.HttpsError("not-found", "Sözleşme bulunamadı.");
    }

    const contract = contractSnap.data()!;

    // Yalnızca taraflar düzenleme talep edebilir
    const isParty = contract.party1Uid === uid || contract.party2Uid === uid;
    if (!isParty) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Yalnızca sözleşmenin tarafları düzenleme talep edebilir."
      );
    }

    if (contract.isLocked) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Kilitlenmiş sözleşme düzenlenemez."
      );
    }

    // Düzenleme talebi yapılabilir durumlar
    const editableStatuses = ["awaiting_party2", "awaiting_approval", "edit_requested"];
    if (!editableStatuses.includes(contract.status)) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        `Sözleşme düzenleme talep edilebilir durumda değil: ${contract.status}`
      );
    }

    const now = admin.firestore.FieldValue.serverTimestamp();
    const otherUid = contract.party1Uid === uid ? contract.party2Uid : contract.party1Uid;
    const requesterName = contract.party1Uid === uid
      ? contract.party1DisplayName
      : contract.party2DisplayName;

    const batch = db.batch();

    // Onayları sıfırla ve durumu güncelle
    batch.update(contractDoc, {
      status: "edit_requested",
      party1Approved: false,
      party1ApprovedAt: null,
      party2Approved: false,
      party2ApprovedAt: null,
      lastEditRequestBy: uid,
      lastEditNote: editNote.trim(),
      updatedAt: now,
    });

    // Diğer tarafa bildirim (party2 yoksa bildirim gönderilmez)
    if (otherUid) {
      const notifRef = db
        .collection("users")
        .doc(otherUid)
        .collection("notifications")
        .doc();

      batch.set(notifRef, {
        id: notifRef.id,
        userId: otherUid,
        type: "contract_edit_requested",
        title: "Sözleşme düzenleme talebi",
        body: `${requesterName} sözleşmede değişiklik yapmak istiyor: "${editNote.trim()}"`,
        isRead: false,
        contractId,
        deepLink: `/contracts/${contractId}`,
        fromUid: uid,
        fromDisplayName: requesterName,
        createdAt: now,
      });
    }

    await batch.commit();

    functions.logger.info(
      `Sözleşme düzenleme talebi: ${contractId} (talep eden: ${uid})`
    );

    return { success: true, contractId };
  });