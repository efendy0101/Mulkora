import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";

const db = admin.firestore();

/**
 * KVKK kapsamında kullanıcı verilerini dışa aktarma talebini kaydeder.
 *
 * İşlem sırası:
 * 1. Oturum doğrulama
 * 2. Aktif bir talepte bulunulmadığını kontrol etme (spam önleme)
 * 3. `dataExportRequests` koleksiyonuna belge oluşturma
 * 4. Admin bildirimi için `adminTasks` koleksiyonuna görev ekleme
 *
 * CLAUDE.md §KVKK: Kullanıcılar kişisel verilerinin kopyasını talep edebilir.
 * docs/22-development-phases.md — Faz 10
 */
export const requestDataExport = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError("unauthenticated", "Kimlik doğrulama gereklidir.");
  }

  const requestsRef = db.collection("dataExportRequests");

  // Son 30 gün içinde aktif bir talep var mı? (spam önleme)
  const thirtyDaysAgo = new Date();
  thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);

  const existing = await requestsRef
    .where("uid", "==", uid)
    .where("status", "in", ["pending", "processing"])
    .where("createdAt", ">=", thirtyDaysAgo)
    .limit(1)
    .get();

  if (!existing.empty) {
    throw new HttpsError(
      "already-exists",
      "Son 30 gün içinde zaten bir veri dışa aktarma talebiniz mevcut. " +
        "Talepler işlendikten sonra yeniden talepte bulunabilirsiniz."
    );
  }

  const batch = db.batch();

  // Veri dışa aktarma talebi belgesi
  const exportRef = requestsRef.doc();
  batch.set(exportRef, {
    uid,
    status: "pending",
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    completedAt: null,
    downloadUrl: null,
    expiresAt: null,
  });

  // Admin görev kuyruğuna ekle
  const taskRef = db.collection("adminTasks").doc();
  batch.set(taskRef, {
    type: "data_export_request",
    uid,
    exportRequestId: exportRef.id,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    status: "pending",
  });

  await batch.commit();

  return { success: true, requestId: exportRef.id };
});