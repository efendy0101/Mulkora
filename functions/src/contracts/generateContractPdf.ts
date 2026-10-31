import * as admin from "../firebaseAdmin";
import * as functions from "firebase-functions/v1";
// pdfkit CJS modülü için TypeScript CJS import sözdizimi
import PDFDocument = require("pdfkit");
import * as crypto from "crypto";

const db = admin.firestore();
const storage = admin.storage();

/**
 * Kilitlenmiş sözleşme için PDF üretir ve Firebase Storage'a yükler.
 * docs/22-development-phases.md Faz 9 — CF-CONTRACT-003
 * CLAUDE.md §14: PDF Özellikleri
 *
 * PDF içeriği:
 * - Sözleşme başlığı
 * - Taraf bilgileri (ad, soyad)
 * - Sözleşme maddeleri
 * - Demirbaş listesi
 * - Oluşturulma tarihi
 * - Sözleşme sürümü
 * - Benzersiz belge numarası
 * - Belge doğrulama kodu
 * - Belge bütünlük hash değeri
 * - Sayfa numaraları
 *
 * Güvenlik kontrolleri:
 * - Sözleşme "locked" durumunda olmalı
 * - Yalnızca taraflar veya yönetici çağırabilir
 * - İdempotent: PDF zaten varsa signed URL döner
 *
 * PDF Storage yolu: contracts/{contractId}/contract_v{version}.pdf
 * Erişim: Storage Security Rules — yalnızca taraflar okuyabilir
 */
export const generateContractPdf = functions
  .region("europe-west1")
  .runWith({ memory: "512MB", timeoutSeconds: 120 })
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

    // Taraf kontrolü
    const isParty = contract.party1Uid === uid || contract.party2Uid === uid;
    const isAdmin = context.auth.token?.admin === true;
    if (!isParty && !isAdmin) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Yalnızca sözleşmenin tarafları PDF indirebilir."
      );
    }

    // Sözleşme kilitli olmalı
    if (!contract.isLocked || contract.status !== "locked") {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "PDF yalnızca kilitlenmiş sözleşmeler için oluşturulabilir."
      );
    }

    // İdempotent: PDF zaten oluşturulduysa signed URL döndür
    if (contract.pdfStoragePath && contract.pdfStatus === "ready") {
      const signedUrl = await generateSignedUrl(contract.pdfStoragePath);
      return { success: true, contractId, pdfUrl: signedUrl, cached: true };
    }

    functions.logger.info(`PDF oluşturuluyor: ${contractId}`);

    // --- PDF oluştur ---
    const pdfBuffer = await buildPdf(contract);

    // --- SHA-256 hash kontrolü ---
    const pdfHash = crypto.createHash("sha256").update(pdfBuffer).digest("hex");

    // --- Firebase Storage'a yükle ---
    const storagePath = `contracts/${contractId}/contract_v${contract.version}.pdf`;
    const bucket = storage.bucket();
    const file = bucket.file(storagePath);

    await file.save(pdfBuffer, {
      metadata: {
        contentType: "application/pdf",
        metadata: {
          contractId,
          documentNumber: contract.documentNumber,
          verificationCode: contract.verificationCode,
          integrityHash: contract.integrityHash,
          pdfHash,
          version: String(contract.version),
          party1Uid: contract.party1Uid,
          party2Uid: contract.party2Uid ?? "",
        },
      },
    });

    // --- Firestore'u güncelle ---
    const now = admin.firestore.FieldValue.serverTimestamp();
    await contractDoc.update({
      pdfStoragePath: storagePath,
      pdfHash,
      pdfStatus: "ready",
      pdfGeneratedAt: now,
      updatedAt: now,
    });

    // --- Her iki tarafa bildirim ---
    const batch = db.batch();
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
        type: "contract_pdf_ready",
        title: "Sözleşme PDF'i hazır",
        body: "Kira sözleşmenizin PDF dosyası oluşturuldu. İndirebilirsiniz.",
        isRead: false,
        contractId,
        deepLink: `/contracts/${contractId}`,
        createdAt: now,
      });
    }
    await batch.commit();

    // --- Signed URL üret (1 saat geçerli) ---
    const signedUrl = await generateSignedUrl(storagePath);

    functions.logger.info(
      `PDF oluşturuldu: ${contractId} (${pdfBuffer.length} bytes)`
    );

    return { success: true, contractId, pdfUrl: signedUrl, cached: false };
  });

/**
 * Firebase Storage dosyası için geçici signed URL üretir.
 * URL 1 saat geçerlidir. Kullanıcı indirdikten sonra süre dolar.
 */
async function generateSignedUrl(storagePath: string): Promise<string> {
  const bucket = storage.bucket();
  const file = bucket.file(storagePath);
  const [url] = await file.getSignedUrl({
    action: "read",
    expires: Date.now() + 60 * 60 * 1000, // 1 saat
  });
  return url;
}

/**
 * Sözleşme verisinden PDF buffer üretir.
 * CLAUDE.md §14 PDF Özellikleri'ne göre tasarlanmıştır.
 */
function buildPdf(contract: FirebaseFirestore.DocumentData): Promise<Buffer> {
  return new Promise((resolve, reject) => {
    const doc = new PDFDocument({
      size: "A4",
      margins: { top: 60, bottom: 60, left: 60, right: 60 },
      info: {
        Title: `Kira Sözleşmesi - ${contract.documentNumber}`,
        Author: "Mülkora",
        Subject: "Kira Sözleşmesi",
        CreationDate: new Date(),
      },
    });

    const chunks: Buffer[] = [];
    doc.on("data", (chunk: Buffer) => chunks.push(chunk));
    doc.on("end", () => resolve(Buffer.concat(chunks)));
    doc.on("error", reject);

    const pageWidth = doc.page.width - 120; // margins

    // =========================================
    // BAŞLIK
    // =========================================
    doc
      .fontSize(20)
      .font("Helvetica-Bold")
      .text("KİRA SÖZLEŞMESİ", { align: "center" })
      .moveDown(0.3);

    doc
      .fontSize(10)
      .font("Helvetica")
      .fillColor("#666666")
      .text("Mülkora Platformu Üzerinden Oluşturulmuştur", { align: "center" })
      .fillColor("#000000")
      .moveDown(1.5);

    // =========================================
    // BELGE BİLGİLERİ KUTUSU
    // =========================================
    drawSection(doc, "BELGE BİLGİLERİ");
    doc.fontSize(10).font("Helvetica");
    infoRow(doc, "Belge Numarası", contract.documentNumber);
    infoRow(doc, "Doğrulama Kodu", contract.verificationCode);
    infoRow(doc, "Sözleşme Sürümü", `v${contract.version}`);
    infoRow(doc, "Oluşturulma Tarihi", formatDate(new Date()));
    infoRow(doc, "Bütünlük Hash (SHA-256)", shortenHash(contract.integrityHash));
    doc.moveDown(1);

    // =========================================
    // TARAF 1 — EV SAHİBİ veya KİRACI (oluşturan)
    // =========================================
    drawSection(doc, "1. TARAF BİLGİLERİ");
    doc.fontSize(10).font("Helvetica");
    infoRow(doc, "Ad Soyad", contract.party1DisplayName ?? "-");
    infoRow(doc, "Kullanıcı ID", contract.party1Uid);
    infoRow(doc, "Onay Tarihi", formatTimestamp(contract.party1ApprovedAt));
    doc.moveDown(1);

    // =========================================
    // TARAF 2
    // =========================================
    drawSection(doc, "2. TARAF BİLGİLERİ");
    doc.fontSize(10).font("Helvetica");
    infoRow(doc, "Ad Soyad", contract.party2DisplayName ?? "-");
    infoRow(doc, "Kullanıcı ID", contract.party2Uid ?? "-");
    infoRow(doc, "Onay Tarihi", formatTimestamp(contract.party2ApprovedAt));
    doc.moveDown(1);

    // =========================================
    // KİRALANAN TAŞINMAZ
    // =========================================
    drawSection(doc, "KIRALANAN TAŞINMAZ");
    doc.fontSize(10).font("Helvetica");
    infoRow(doc, "Adres", contract.propertyAddress);
    infoRow(doc, "Taşınmaz Türü", contract.propertyType);
    doc.moveDown(1);

    // =========================================
    // KİRA KOŞULLARI
    // =========================================
    drawSection(doc, "KİRA KOŞULLARI");
    doc.fontSize(10).font("Helvetica");
    infoRow(doc, "Aylık Kira Bedeli", `${formatAmount(contract.monthlyRent)} TL`);
    infoRow(doc, "Ödeme Günü", `Her ayın ${contract.paymentDay}. günü`);
    infoRow(doc, "Ödeme Yöntemi", contract.paymentMethod);
    infoRow(doc, "Depozito", `${formatAmount(contract.depositAmount)} TL`);
    if (contract.monthlyFee) {
      infoRow(doc, "Aylık Aidat", `${formatAmount(contract.monthlyFee)} TL`);
    }
    infoRow(doc, "Kira Başlangıç Tarihi", contract.startDate);
    infoRow(doc, "Kira Bitiş Tarihi", contract.endDate);
    doc.moveDown(1);

    // =========================================
    // FATURALAR
    // =========================================
    if (contract.utilityResponsibilities) {
      drawSection(doc, "FATURA SORUMLULUKLARI");
      doc.fontSize(10).font("Helvetica")
        .text(JSON.stringify(contract.utilityResponsibilities, null, 2))
        .moveDown(1);
    }

    // =========================================
    // DEMİRBAŞ LİSTESİ
    // =========================================
    if (contract.inventoryItems && contract.inventoryItems.length > 0) {
      doc.addPage();
      drawSection(doc, "DEMİRBAŞ LİSTESİ");
      doc.fontSize(10).font("Helvetica");
      contract.inventoryItems.forEach((item: string, idx: number) => {
        doc.text(`${idx + 1}. ${item}`);
      });
      doc.moveDown(1);
    }

    // =========================================
    // ÖZEL ŞARTLAR
    // =========================================
    if (contract.specialConditions) {
      drawSection(doc, "ÖZEL ŞARTLAR");
      doc
        .fontSize(10)
        .font("Helvetica")
        .text(contract.specialConditions, { lineGap: 4 })
        .moveDown(1);
    }

    // =========================================
    // KEFİL BİLGİLERİ
    // =========================================
    if (contract.guarantorInfo) {
      drawSection(doc, "KEFİL BİLGİLERİ");
      doc.fontSize(10).font("Helvetica");
      const g = contract.guarantorInfo;
      if (typeof g === "object") {
        Object.entries(g).forEach(([k, v]) => infoRow(doc, k, String(v)));
      } else {
        doc.text(String(g));
      }
      doc.moveDown(1);
    }

    // =========================================
    // İMZA ALANLARI
    // =========================================
    doc.moveDown(2);
    drawSection(doc, "TARAF ONAYLARI (Dijital)");
    doc.fontSize(9).font("Helvetica").fillColor("#666666");
    doc.text(
      "Bu sözleşme Mülkora platformu üzerinden her iki tarafın dijital onayıyla " +
      "oluşturulmuştur. Elektronik onay tarihleri belge bilgilerinde yer almaktadır.",
      { lineGap: 4 }
    );
    doc.moveDown(0.5);
    doc.fillColor("#000000")
      .text(`1. Taraf Onayı: ${formatTimestamp(contract.party1ApprovedAt)}`)
      .text(`2. Taraf Onayı: ${formatTimestamp(contract.party2ApprovedAt)}`);

    // =========================================
    // SAYFA NUMARALARI
    // =========================================
    const range = doc.bufferedPageRange();
    for (let i = 0; i < range.count; i++) {
      doc.switchToPage(range.start + i);
      doc
        .fontSize(8)
        .fillColor("#999999")
        .text(
          `Sayfa ${i + 1} / ${range.count}   |   ${contract.documentNumber}   |   Mülkora`,
          60,
          doc.page.height - 40,
          { width: pageWidth, align: "center" }
        );
    }

    doc.end();
  });
}

// --- Yardımcı fonksiyonlar ---

function drawSection(doc: PDFKit.PDFDocument, title: string): void {
  doc
    .moveDown(0.5)
    .fontSize(11)
    .font("Helvetica-Bold")
    .fillColor("#1a1a2e")
    .text(title.toUpperCase())
    .moveDown(0.3)
    .moveTo(60, doc.y)
    .lineTo(doc.page.width - 60, doc.y)
    .strokeColor("#cccccc")
    .stroke()
    .moveDown(0.5)
    .fillColor("#000000");
}

function infoRow(doc: PDFKit.PDFDocument, label: string, value: string): void {
  const labelWidth = 180;
  const x = doc.x;
  const y = doc.y;
  doc.font("Helvetica-Bold").fontSize(10).text(label + ":", x, y, { width: labelWidth, continued: false });
  doc.font("Helvetica").fontSize(10).text(value, x + labelWidth, y, { lineBreak: false });
  doc.moveDown(0.5);
}

function formatDate(date: Date): string {
  return date.toLocaleDateString("tr-TR", {
    year: "numeric",
    month: "long",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function formatTimestamp(ts: admin.firestore.Timestamp | null | undefined): string {
  if (!ts) return "-";
  if (typeof ts.toDate === "function") {
    return formatDate(ts.toDate());
  }
  return String(ts);
}

function formatAmount(amount: number): string {
  return amount.toLocaleString("tr-TR", { minimumFractionDigits: 2 });
}

function shortenHash(hash: string): string {
  if (!hash) return "-";
  return `${hash.slice(0, 16)}...${hash.slice(-8)}`;
}