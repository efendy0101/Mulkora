import * as admin from "firebase-admin";
import * as functions from "firebase-functions";
import * as crypto from "crypto";

const db = admin.firestore();

/**
 * Her iki taraf onayladıktan sonra sözleşmeyi kilitler.
 * docs/22-development-phases.md Faz 9 — CF-CONTRACT-002
 *
 * Bu fonksiyon `approveContract` tarafından "both_approved" durumuna geçince
 * istemciden çağrılır veya Firestore trigger ile otomatik tetiklenebilir.
 *
 * Akış:
 * 1. Sözleşmenin "both_approved" durumunda olduğunu doğrular
 * 2. Sözleşmenin bütünlük hash'ini hesaplar (SHA-256)
 * 3. isLocked = true, status = "locked" yapar
 * 4. Sürüm snapshot kaydeder
 * 5. generateContractPdf çağrılması için PDF bekleniyor durumuna geçer
 * 6. Her iki tarafa bildirim gönderir
 *
 * Güvenlik kontrolleri:
 * - Yalnızca taraflar veya sistem (admin SDK) çağırabilir
 * - Sözleşme "both_approved" durumunda olmalı
 * - İdempotent: zaten kilitliyse sessizce başarı döner
 */
export const lockContractVersion = functions
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
    const contractSnap = await contractDoc.get();

    if (!contractSnap.exists) {
      throw new functions.https.HttpsError("not-found", "Sözleşme bulunamadı.");
    }

    const contract = contractSnap.data()!;

    // İdempotent: zaten kilitliyse başarı döndür
    if (contract.isLocked) {
      return { success: true, contractId, alreadyLocked: true };
    }

    // Taraf kontrolü
    const isParty = contract.party1Uid === uid || contract.party2Uid === uid;
    const isAdmin = context.auth.token?.admin === true;
    if (!isParty && !isAdmin) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Yalnızca sözleşmenin tarafları veya yönetici kilitleme işlemi yapabilir."
      );
    }

    // Durum kontrolü: yalnızca "both_approved" kilitlenebilir
    if (contract.status !== "both_approved") {
      throw new functions.https.HttpsError(
        "failed-precondition",
        `Sözleşme kilitlenebilir durumda değil: ${contract.status}. Her iki tarafın onaylaması gerekir.`
      );
    }

    // --- Bütünlük hash'i hesapla ---
    const hashInput = buildHashInput(contract);
    const integrityHash = crypto
      .createHash("sha256")
      .update(hashInput)
      .digest("hex");

    const now = admin.firestore.FieldValue.serverTimestamp();
    const newVersion = (contract.version ?? 1) + 1;
    const batch = db.batch();

    // --- Sözleşmeyi kilitle ---
    batch.update(contractDoc, {
      status: "locked",
      isLocked: true,
      lockedAt: now,
      lockedBy: uid,
      integrityHash,
      version: newVersion,
      pdfStatus: "pending",
      updatedAt: now,
    });

    // --- Sürüm snapshot'ı kaydet ---
    const versionRef = contractDoc
      .collection("versions")
      .doc(`v${newVersion}`);

    batch.set(versionRef, {
      version: newVersion,
      snapshotData: { ...contract, status: "locked", isLocked: true, integrityHash },
      createdBy: uid,
      createdAt: now,
      changeNote: "Sözleşme kilitlendi. Her iki taraf onayladı.",
    });

    // --- Her iki tarafa bildirim ---
    for (const targetUid of [contract.party1Uid, contract.party2Uid]) {
      if (!targetUid) continue;
      const notifRef = db
        .collection("users")
        .doc(targetUid)
        .collection("notifications")
        .doc();

      batch.set(notifRef, {
        id: notifRef.id,
        userId: targetUid,
        type: "contract_locked",
        title: "Sözleşme kilitlendi",
        body: "Sözleşme her iki tarafça onaylandı ve kilitlendi. PDF hazırlanıyor.",
        isRead: false,
        contractId,
        deepLink: `/contracts/${contractId}`,
        createdAt: now,
      });
    }

    await batch.commit();

    functions.logger.info(
      `Sözleşme kilitlendi: ${contractId} (hash: ${integrityHash.slice(0, 12)}...)`
    );

    return {
      success: true,
      contractId,
      alreadyLocked: false,
      integrityHash,
    };
  });

/**
 * Sözleşme verilerinden deterministik hash girişi oluşturur.
 * Yalnızca sözleşme içeriğini (meta veriler hariç) kapsar.
 */
function buildHashInput(contract: FirebaseFirestore.DocumentData): string {
  const fields = {
    propertyAddress: contract.propertyAddress,
    propertyType: contract.propertyType,
    monthlyRent: contract.monthlyRent,
    paymentDay: contract.paymentDay,
    paymentMethod: contract.paymentMethod,
    depositAmount: contract.depositAmount,
    startDate: contract.startDate,
    endDate: contract.endDate,
    monthlyFee: contract.monthlyFee ?? null,
    utilityResponsibilities: contract.utilityResponsibilities ?? null,
    specialConditions: contract.specialConditions ?? null,
    guarantorInfo: contract.guarantorInfo ?? null,
    inventoryItems: contract.inventoryItems ?? [],
    party1Uid: contract.party1Uid,
    party2Uid: contract.party2Uid,
    documentNumber: contract.documentNumber,
    verificationCode: contract.verificationCode,
  };
  return JSON.stringify(fields, Object.keys(fields).sort());
}