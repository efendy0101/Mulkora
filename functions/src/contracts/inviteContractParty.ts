import * as admin from "../firebaseAdmin";
import * as functions from "firebase-functions/v1";

const db = admin.firestore();

/**
 * Sözleşmeye ikinci tarafı davet eder.
 * docs/22-development-phases.md Faz 9 — inviteContractParty
 *
 * Akış:
 * 1. Sözleşmenin "draft" durumunda ve ikinci tarafın henüz eklenmemiş olduğunu doğrular
 * 2. Hedef kullanıcının varlığını kontrol eder
 * 3. Hedef kullanıcıyı sözleşmenin party2 alanına ekler
 * 4. Sözleşme durumunu "awaiting_party2" olarak günceller
 * 5. Hedef kullanıcıya bildirim gönderir
 *
 * Güvenlik kontrolleri:
 * - Yalnızca sözleşmeyi oluşturan kullanıcı (party1) davet gönderebilir
 * - Sözleşme kilitli olmamalıdır
 * - Kullanıcı kendini davet edemez
 */
export const inviteContractParty = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Bu işlem için giriş yapılmış olmalıdır."
      );
    }

    const inviterUid = context.auth.uid;
    const { contractId, inviteeUid } = data;

    if (!contractId || typeof contractId !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Geçerli bir contractId gereklidir."
      );
    }

    if (!inviteeUid || typeof inviteeUid !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Geçerli bir inviteeUid gereklidir."
      );
    }

    if (inviterUid === inviteeUid) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Kendinizi sözleşmeye davet edemezsiniz."
      );
    }

    // --- Sözleşmeyi al ---
    const contractDoc = db.collection("contracts").doc(contractId);
    const contractSnap = await contractDoc.get();

    if (!contractSnap.exists) {
      throw new functions.https.HttpsError("not-found", "Sözleşme bulunamadı.");
    }

    const contract = contractSnap.data()!;

    // Yalnızca party1 davet gönderebilir
    if (contract.party1Uid !== inviterUid) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Yalnızca sözleşmeyi oluşturan kişi davet gönderebilir."
      );
    }

    // Sözleşme kilitli olmamalı
    if (contract.isLocked) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Kilitlenmiş sözleşmeye davet gönderilemez."
      );
    }

    // Sözleşme "draft" veya "awaiting_party2" durumunda olmalı
    if (!["draft", "awaiting_party2"].includes(contract.status)) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        `Sözleşme davet gönderilebilir durumda değil: ${contract.status}`
      );
    }

    // Zaten bir party2 varsa ve farklı bir kullanıcıysa hata ver
    if (contract.party2Uid && contract.party2Uid !== inviteeUid) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Sözleşmede zaten farklı bir ikinci taraf mevcut."
      );
    }

    // --- Davet edilen kullanıcı bilgilerini al ---
    const inviteeSnap = await db.collection("users").doc(inviteeUid).get();
    if (!inviteeSnap.exists) {
      throw new functions.https.HttpsError("not-found", "Davet edilmek istenen kullanıcı bulunamadı.");
    }

    const invitee = inviteeSnap.data()!;
    const now = admin.firestore.FieldValue.serverTimestamp();
    const batch = db.batch();

    // --- Sözleşmeyi güncelle ---
    batch.update(contractDoc, {
      status: "awaiting_party2",
      party2Uid: inviteeUid,
      party2DisplayName: invitee.displayName ?? "",
      party2PhotoUrl: invitee.photoUrl ?? null,
      party2Approved: false,
      party2ApprovedAt: null,
      // partyUids array'ine inviteeUid ekle — array-contains sorgusu ve Security Rules için
      partyUids: admin.firestore.FieldValue.arrayUnion(inviteeUid),
      updatedAt: now,
    });

    // --- Davet edilen kullanıcıya bildirim ---
    const notifRef = db
      .collection("users")
      .doc(inviteeUid)
      .collection("notifications")
      .doc();

    batch.set(notifRef, {
      id: notifRef.id,
      userId: inviteeUid,
      type: "contract_invite",
      title: "Kira sözleşmesi daveti",
      body: `${contract.party1DisplayName} sizi bir kira sözleşmesine davet etti.`,
      isRead: false,
      contractId,
      deepLink: `/contracts/${contractId}`,
      fromUid: inviterUid,
      fromDisplayName: contract.party1DisplayName,
      fromPhotoUrl: contract.party1PhotoUrl ?? null,
      createdAt: now,
    });

    await batch.commit();

    functions.logger.info(
      `Sözleşme daveti gönderildi: ${contractId} → ${inviteeUid} (gönderen: ${inviterUid})`
    );

    return { success: true, contractId };
  });