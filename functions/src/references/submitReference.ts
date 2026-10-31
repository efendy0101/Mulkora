import * as admin from "firebase-admin";
import * as functions from "firebase-functions";
import { checkContentFilter } from "../moderation/contentFilter";

const db = admin.firestore();

/**
 * Referans içeriğini gönderir ve moderatör onayına sunar.
 * docs/16-reference-system-state-machine.md §3 — Geçiş Tablosu
 * Durum geçişi: preparing → pending_approval  (CF-REF-003)
 *
 * Güvenlik kontrolleri:
 * - Yalnızca referansı başlatan kişi (fromUid) gönderebilir
 * - Referans "preparing" durumunda olmalıdır
 * - Rating 1-5 arasında olmalıdır
 * - İçerik en az 20, en fazla 2000 karakter olmalıdır
 * - Tüm zorunlu alanlar dolu olmalıdır
 * - İçerik filtresi uygulanır (docs/18-content-filter-design.md)
 *
 * Yazar, preparing aşamasında içeriği güncelleyebilir; bu CF son hâlini kaydeder.
 */
export const submitReference = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Bu işlem için giriş yapılmış olmalıdır."
      );
    }

    const uid = context.auth.uid;
    const {
      referenceId,
      rating,
      content,
      relationshipType,
      propertyAddress,
      startDate,
      endDate,
    } = data;

    // --- Alan doğrulamaları ---
    if (!referenceId || typeof referenceId !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Geçerli bir referenceId gereklidir."
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

    if (content.trim().length > 2000) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Referans içeriği en fazla 2000 karakter olabilir."
      );
    }

    // --- Belgeyi yükle ---
    const refDoc = db.collection("references").doc(referenceId);
    const refSnap = await refDoc.get();

    if (!refSnap.exists) {
      throw new functions.https.HttpsError("not-found", "Referans bulunamadı.");
    }

    const ref = refSnap.data()!;

    // Yalnızca referansı başlatan kişi gönderebilir
    if (ref.fromUid !== uid) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Bu referansı yalnızca referansı başlatan kişi gönderebilir."
      );
    }

    // Yalnızca "preparing" durumunda gönderilebilir
    if (ref.status !== "preparing") {
      throw new functions.https.HttpsError(
        "failed-precondition",
        `Referans "preparing" durumunda değil: ${ref.status}`
      );
    }

    // --- İçerik filtresi (docs/18-content-filter-design.md) ---
    // HIGH risk → anında reddet; MEDIUM → moderasyon kuyruğuna ekle ve devam et
    const filterResult = await checkContentFilter(content.trim());
    if (filterResult.riskLevel === "HIGH") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        `Referans içeriği uygunsuz ifadeler içeriyor: ${filterResult.matchedTerms.join(", ")}`
      );
    }

    const now = admin.firestore.FieldValue.serverTimestamp();
    const batch = db.batch();

    // preparing → pending_approval
    batch.update(refDoc, {
      status: "pending_approval",
      rating,
      content: content.trim(),
      relationshipType: relationshipType ?? ref.relationshipType ?? null,
      propertyAddress: propertyAddress ?? ref.propertyAddress ?? null,
      startDate: startDate ?? ref.startDate ?? null,
      endDate: endDate ?? ref.endDate ?? null,
      submittedAt: now,
      updatedAt: now,
    });

    // Hedef kullanıcıya bildirim
    const notifRef = db
      .collection("users")
      .doc(ref.toUid)
      .collection("notifications")
      .doc();

    batch.set(notifRef, {
      id: notifRef.id,
      userId: ref.toUid,
      type: "reference_submitted",
      title: "Hakkınızda referans gönderildi",
      body: `${ref.fromDisplayName} adlı kullanıcı hakkınızda referans yazdı. Onay sürecinde inceleniyor.`,
      isRead: false,
      referenceId,
      deepLink: `/references/${referenceId}`,
      fromUid: uid,
      fromDisplayName: ref.fromDisplayName,
      fromPhotoUrl: ref.fromPhotoUrl ?? null,
      createdAt: now,
    });

    await batch.commit();

    functions.logger.info(
      `Referans incelemeye gönderildi: ${referenceId} (fromUid: ${uid})`
    );

    return { success: true, referenceId };
  });