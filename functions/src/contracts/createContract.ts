import * as admin from "firebase-admin";
import * as functions from "firebase-functions";
import * as crypto from "crypto";

const db = admin.firestore();

/**
 * Kira sözleşmesi taslağı oluşturur.
 * docs/22-development-phases.md Faz 9 — CF-CONTRACT-001
 *
 * Akış:
 * 1. Kullanıcı kimliği ve temel sözleşme bilgilerini doğrular
 * 2. Benzersiz belge numarası ve doğrulama kodu oluşturur
 * 3. Sözleşmeyi "draft" durumunda Firestore'a kaydeder
 * 4. Sözleşme sürüm kaydını oluşturur (v1)
 *
 * Güvenlik kontrolleri:
 * - Kimlik doğrulama zorunlu
 * - Oluşturan kullanıcı otomatik olarak taraf 1 olur
 * - Yönetici/moderatör rolü kısıtlaması YOK (tüm roller sözleşme oluşturabilir)
 */
export const createContract = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Bu işlem için giriş yapılmış olmalıdır."
      );
    }

    const creatorUid = context.auth.uid;
    const {
      templateId,
      propertyAddress,
      propertyType,
      monthlyRent,
      paymentDay,
      paymentMethod,
      depositAmount,
      startDate,
      endDate,
      // İsteğe bağlı alanlar
      monthlyFee,
      utilityResponsibilities,
      specialConditions,
      guarantorInfo,
      inventoryItems,
    } = data;

    // --- Zorunlu alan doğrulamaları ---
    if (!propertyAddress || typeof propertyAddress !== "string" || propertyAddress.trim().length < 10) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Geçerli bir taşınmaz adresi girilmelidir (en az 10 karakter)."
      );
    }

    if (!propertyType || typeof propertyType !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Taşınmaz türü belirtilmelidir."
      );
    }

    if (typeof monthlyRent !== "number" || monthlyRent <= 0) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Geçerli bir aylık kira bedeli girilmelidir."
      );
    }

    if (typeof paymentDay !== "number" || paymentDay < 1 || paymentDay > 31) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Ödeme günü 1 ile 31 arasında olmalıdır."
      );
    }

    if (!paymentMethod || typeof paymentMethod !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Ödeme yöntemi belirtilmelidir."
      );
    }

    if (typeof depositAmount !== "number" || depositAmount < 0) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Geçerli bir depozito miktarı girilmelidir."
      );
    }

    if (!startDate || typeof startDate !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Kira başlangıç tarihi belirtilmelidir (YYYY-MM-DD)."
      );
    }

    if (!endDate || typeof endDate !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Kira bitiş tarihi belirtilmelidir (YYYY-MM-DD)."
      );
    }

    // Tarih doğrulaması
    const start = new Date(startDate);
    const end = new Date(endDate);
    if (isNaN(start.getTime()) || isNaN(end.getTime())) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Tarihler YYYY-MM-DD formatında olmalıdır."
      );
    }
    if (end <= start) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Bitiş tarihi başlangıç tarihinden sonra olmalıdır."
      );
    }

    // --- Oluşturucu kullanıcı bilgilerini al ---
    const creatorSnap = await db.collection("users").doc(creatorUid).get();
    if (!creatorSnap.exists) {
      throw new functions.https.HttpsError("not-found", "Kullanıcı bulunamadı.");
    }

    const creator = creatorSnap.data()!;

    // --- Benzersiz belge numarası ve doğrulama kodu ---
    const docNumber = generateDocumentNumber();
    const verificationCode = generateVerificationCode();
    const now = admin.firestore.FieldValue.serverTimestamp();

    const contractRef = db.collection("contracts").doc();
    const contractId = contractRef.id;

    const contractData = {
      id: contractId,
      status: "draft",
      templateId: templateId ?? null,

      // Taşınmaz bilgileri
      propertyAddress: propertyAddress.trim(),
      propertyType,

      // Finansal bilgiler
      monthlyRent,
      paymentDay,
      paymentMethod,
      depositAmount,
      monthlyFee: monthlyFee ?? null,

      // Tarihler
      startDate,
      endDate,

      // Yükümlülükler
      utilityResponsibilities: utilityResponsibilities ?? null,
      specialConditions: specialConditions?.trim() ?? null,
      guarantorInfo: guarantorInfo ?? null,
      inventoryItems: inventoryItems ?? [],

      // Taraflar
      party1Uid: creatorUid,
      party1DisplayName: creator.displayName ?? "",
      party1PhotoUrl: creator.photoUrl ?? null,
      party1Approved: false,
      party1ApprovedAt: null,

      party2Uid: null,
      party2DisplayName: null,
      party2PhotoUrl: null,
      party2Approved: false,
      party2ApprovedAt: null,

      // Firestore array-contains sorgusu ve Security Rules için (partyUids CONTAINS uid)
      // Bkz. firestore.indexes.json + firestore.rules: resource.data.partyUids
      partyUids: [creatorUid],

      // Belge meta
      documentNumber: docNumber,
      verificationCode,
      version: 1,
      isLocked: false,
      pdfStoragePath: null,
      pdfGeneratedAt: null,

      // Zaman damgaları
      createdBy: creatorUid,
      createdAt: now,
      updatedAt: now,
    };

    const batch = db.batch();

    // Sözleşmeyi oluştur
    batch.set(contractRef, contractData);

    // İlk sürüm kaydı
    const versionRef = db
      .collection("contracts")
      .doc(contractId)
      .collection("versions")
      .doc("v1");

    batch.set(versionRef, {
      version: 1,
      snapshotData: contractData,
      createdBy: creatorUid,
      createdAt: now,
      changeNote: "İlk taslak oluşturuldu.",
    });

    await batch.commit();

    functions.logger.info(
      `Sözleşme taslağı oluşturuldu: ${contractId} (oluşturan: ${creatorUid})`
    );

    return { success: true, contractId, documentNumber: docNumber };
  });

/**
 * Benzersiz belge numarası üretir.
 * Format: MLK-YYYYMMDD-XXXXXX (6 haneli rastgele hex)
 */
function generateDocumentNumber(): string {
  const now = new Date();
  const datePart = now.toISOString().slice(0, 10).replace(/-/g, "");
  const randomPart = crypto.randomBytes(3).toString("hex").toUpperCase();
  return `MLK-${datePart}-${randomPart}`;
}

/**
 * 8 karakterli alfanümerik doğrulama kodu üretir.
 */
function generateVerificationCode(): string {
  return crypto.randomBytes(4).toString("hex").toUpperCase();
}