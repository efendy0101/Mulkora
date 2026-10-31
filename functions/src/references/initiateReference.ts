import * as admin from "firebase-admin";
import * as functions from "firebase-functions";

const db = admin.firestore();

/**
 * Referans daveti başlatır.
 * docs/16-reference-system-state-machine.md
 * Durum geçişi: (yok) → invite_sent → awaiting_confirmation
 *
 * Güvenlik kontrolleri:
 * - Kullanıcı kendi kendine referans veremez
 * - E-posta doğrulanmış olmalıdır
 * - Aynı çift arasında aktif referans yoksa devam eder
 * - Rating 1-5 arasında olmalıdır
 * - İçerik en az 20 karakter olmalıdır
 * - Rate limit: 24 saatte 5 talep
 *
 * docs/16 §7 — aktif durumlar:
 * invite_sent, awaiting_confirmation, preparing, pending_approval,
 * approved, appealed, under_review, hidden
 */

const ACTIVE_STATUSES = [
  "invite_sent",
  "awaiting_confirmation",
  "preparing",
  "pending_approval",
  "approved",
  "appealed",
  "under_review",
  "hidden",
];

export const initiateReference = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Bu işlem için giriş yapılmış olmalıdır."
      );
    }

    const fromUid = context.auth.uid;
    const {
      toUid,
      referenceType,
      rating,
      content,
      relationshipType,
      propertyAddress,
      startDate,
      endDate,
    } = data;

    // --- Doğrulamalar ---
    if (!toUid || typeof toUid !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Geçerli bir toUid gereklidir."
      );
    }

    if (fromUid === toUid) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Kendinize referans veremezsiniz."
      );
    }

    if (!rating || typeof rating !== "number" || rating < 1 || rating > 5) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Değerlendirme 1 ile 5 arasında olmalıdır."
      );
    }

    if (!content || typeof content !== "string" || content.trim().length < 20) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Referans içeriği en az 20 karakter olmalıdır."
      );
    }

    if (
      !referenceType ||
      !["tenant_to_landlord", "landlord_to_tenant"].includes(referenceType)
    ) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Geçersiz referans türü."
      );
    }

    // E-posta doğrulama
    const userRecord = await admin.auth().getUser(fromUid);
    if (!userRecord.emailVerified) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Referans başlatabilmek için e-postanızı doğrulamanız gereklidir."
      );
    }

    // Rate limit: son 24 saatte 5'ten fazla davet
    const oneDayAgo = new Date(Date.now() - 24 * 60 * 60 * 1000);
    const recentRefs = await db
      .collection("references")
      .where("fromUid", "==", fromUid)
      .where("createdAt", ">=", oneDayAgo)
      .get();

    if (recentRefs.size >= 5) {
      throw new functions.https.HttpsError(
        "resource-exhausted",
        "24 saatte en fazla 5 referans daveti gönderebilirsiniz."
      );
    }

    // Alıcı kullanıcı var mı?
    const toUserSnap = await db.collection("users").doc(toUid).get();
    if (!toUserSnap.exists) {
      throw new functions.https.HttpsError("not-found", "Kullanıcı bulunamadı.");
    }

    // Aktif referans kontrolü: aynı çift arasında aktif referans var mı?
    const existingRef = await db
      .collection("references")
      .where("fromUid", "==", fromUid)
      .where("toUid", "==", toUid)
      .where("status", "in", ACTIVE_STATUSES)
      .limit(1)
      .get();

    if (!existingRef.empty) {
      throw new functions.https.HttpsError(
        "already-exists",
        "Bu kullanıcıya ait aktif bir referans isteği zaten var."
      );
    }

    // Gönderen kullanıcı bilgilerini al
    const fromUserSnap = await db.collection("users").doc(fromUid).get();
    const fromUser = fromUserSnap.data()!;
    const toUser = toUserSnap.data()!;

    const now = admin.firestore.FieldValue.serverTimestamp();
    const batch = db.batch();

    // Referans belgesi oluştur — başlangıç durumu: invite_sent
    const referenceRef = db.collection("references").doc();
    batch.set(referenceRef, {
      id: referenceRef.id,
      fromUid,
      toUid,
      fromUsername: fromUser.username,
      toUsername: toUser.username,
      fromDisplayName: fromUser.displayName,
      toDisplayName: toUser.displayName,
      fromPhotoUrl: fromUser.photoUrl ?? null,
      toPhotoUrl: toUser.photoUrl ?? null,
      referenceType,
      rating,
      content: content.trim(),
      status: "invite_sent",
      relationshipType: relationshipType ?? null,
      propertyAddress: propertyAddress ?? null,
      startDate: startDate ?? null,
      endDate: endDate ?? null,
      response: null,
      createdAt: now,
      updatedAt: now,
    });

    // Alıcıya bildirim gönder
    const notifRef = db
      .collection("users")
      .doc(toUid)
      .collection("notifications")
      .doc();

    batch.set(notifRef, {
      id: notifRef.id,
      userId: toUid,
      type: "reference_invite",
      title: "Yeni referans daveti",
      body: `${fromUser.displayName} sizi tanıyor musunuz diye soruyor.`,
      isRead: false,
      referenceId: referenceRef.id,
      deepLink: `/references/${referenceRef.id}`,
      fromUid,
      fromDisplayName: fromUser.displayName,
      fromPhotoUrl: fromUser.photoUrl ?? null,
      createdAt: now,
    });

    await batch.commit();

    functions.logger.info(
      `Referans daveti başlatıldı: ${referenceRef.id} (${fromUid} → ${toUid})`
    );

    return { success: true, referenceId: referenceRef.id };
  });
