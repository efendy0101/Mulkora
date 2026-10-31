import * as admin from "../firebaseAdmin";
import * as functions from "firebase-functions/v1";

const db = admin.firestore();

/**
 * Sözleşme taraflarından biri sözleşmeyi onaylar.
 * docs/22-development-phases.md Faz 9 — approveContract
 *
 * Durum geçişleri:
 *   awaiting_party2 / edit_requested → awaiting_approval (ilk onay)
 *   awaiting_approval → both_approved (her iki taraf da onayladı)
 *
 * Her iki taraf onayladığında lockContractVersion otomatik tetiklenir.
 * Güvenlik kontrolleri:
 * - Yalnızca taraflar onaylayabilir
 * - Sözleşme kilitli olmamalı
 * - İkinci taraf henüz atanmış olmalı
 */
export const approveContract = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Bu işlem için giriş yapılmış olmalıdır."
      );
    }

    const uid = context.auth.uid;
    const { contractId } = data;

    if (!contractId || typeof contractId !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Geçerli bir contractId gereklidir."
      );
    }

    const contractDoc = db.collection("contracts").doc(contractId);

    // Transaction: atomik onay güncellemesi
    const result = await db.runTransaction(async (tx) => {
      const contractSnap = await tx.get(contractDoc);

      if (!contractSnap.exists) {
        throw new functions.https.HttpsError("not-found", "Sözleşme bulunamadı.");
      }

      const contract = contractSnap.data()!;

      // Taraf kontrolü
      const isParty1 = contract.party1Uid === uid;
      const isParty2 = contract.party2Uid === uid;

      if (!isParty1 && !isParty2) {
        throw new functions.https.HttpsError(
          "permission-denied",
          "Yalnızca sözleşmenin tarafları onaylayabilir."
        );
      }

      if (contract.isLocked) {
        throw new functions.https.HttpsError(
          "failed-precondition",
          "Kilitlenmiş sözleşme zaten onaylanmış."
        );
      }

      // İkinci taraf henüz atanmış olmalı
      if (!contract.party2Uid) {
        throw new functions.https.HttpsError(
          "failed-precondition",
          "Henüz ikinci taraf eklenmemiş. Önce sözleşmeye davet gönderin."
        );
      }

      const approvableStatuses = [
        "awaiting_party2",
        "awaiting_approval",
        "edit_requested",
      ];
      if (!approvableStatuses.includes(contract.status)) {
        throw new functions.https.HttpsError(
          "failed-precondition",
          `Sözleşme onaylanabilir durumda değil: ${contract.status}`
        );
      }

      // Kullanıcı zaten onaylamışsa tekrar onaylamasına gerek yok
      if (isParty1 && contract.party1Approved) {
        throw new functions.https.HttpsError(
          "already-exists",
          "Sözleşmeyi zaten onayladınız."
        );
      }
      if (isParty2 && contract.party2Approved) {
        throw new functions.https.HttpsError(
          "already-exists",
          "Sözleşmeyi zaten onayladınız."
        );
      }

      const now = admin.firestore.FieldValue.serverTimestamp();
      const updateFields: Record<string, unknown> = { updatedAt: now };

      if (isParty1) {
        updateFields["party1Approved"] = true;
        updateFields["party1ApprovedAt"] = now;
      } else {
        updateFields["party2Approved"] = true;
        updateFields["party2ApprovedAt"] = now;
      }

      // Her iki taraf da onayladı mı?
      const party1Approved = isParty1 ? true : contract.party1Approved;
      const party2Approved = isParty2 ? true : contract.party2Approved;
      const bothApproved = party1Approved && party2Approved;

      if (bothApproved) {
        updateFields["status"] = "both_approved";
      } else {
        updateFields["status"] = "awaiting_approval";
      }

      tx.update(contractDoc, updateFields);

      return {
        bothApproved,
        party1Uid: contract.party1Uid,
        party2Uid: contract.party2Uid,
        party1DisplayName: contract.party1DisplayName,
        party2DisplayName: contract.party2DisplayName,
        approverUid: uid,
        approverName: isParty1 ? contract.party1DisplayName : contract.party2DisplayName,
        otherUid: isParty1 ? contract.party2Uid : contract.party1Uid,
        otherName: isParty1 ? contract.party2DisplayName : contract.party1DisplayName,
      };
    });

    // Transaction tamamlandı — bildirim gönder
    const now = admin.firestore.FieldValue.serverTimestamp();

    if (result.bothApproved) {
      // Her iki tarafa "sözleşme onaylandı, kilitlenecek" bildirimi
      const batch = db.batch();

      for (const targetUid of [result.party1Uid, result.party2Uid]) {
        if (!targetUid) continue;
        const notifRef = db
          .collection("users")
          .doc(targetUid)
          .collection("notifications")
          .doc();

        batch.set(notifRef, {
          id: notifRef.id,
          userId: targetUid,
          type: "contract_both_approved",
          title: "Sözleşme her iki tarafça onaylandı",
          body: "Sözleşme kilitlenip PDF oluşturulacak.",
          isRead: false,
          contractId,
          deepLink: `/contracts/${contractId}`,
          createdAt: now,
        });
      }

      await batch.commit();

      functions.logger.info(
        `Sözleşme her iki tarafça onaylandı: ${contractId}`
      );
    } else {
      // Diğer tarafa "onayı bekliyorum" bildirimi
      if (result.otherUid) {
        const notifRef = db
          .collection("users")
          .doc(result.otherUid)
          .collection("notifications")
          .doc();

        await notifRef.set({
          id: notifRef.id,
          userId: result.otherUid,
          type: "contract_awaiting_approval",
          title: "Sözleşme onayınızı bekliyor",
          body: `${result.approverName} sözleşmeyi onayladı. Sözleşmenin geçerli olması için sizin de onaylamanız gerekmektedir.`,
          isRead: false,
          contractId,
          deepLink: `/contracts/${contractId}`,
          fromUid: result.approverUid,
          fromDisplayName: result.approverName,
          createdAt: now,
        });
      }

      functions.logger.info(
        `Sözleşme onayı: ${contractId} (onaylayan: ${uid})`
      );
    }

    return {
      success: true,
      contractId,
      bothApproved: result.bothApproved,
    };
  });