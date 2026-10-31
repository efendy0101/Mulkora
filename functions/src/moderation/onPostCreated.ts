import * as admin from "../firebaseAdmin";
import * as functions from "firebase-functions/v1";
import { analyzeContent } from "./contentFilter";
import { loadBannedTerms } from "./bannedTermsLoader";

/**
 * Gönderi durumu `draft → pending` geçişini izler ve içerik filtresini çalıştırır.
 *
 * CLAUDE.md §12: Küfür, Argo ve Zararlı İçerik Filtresi
 * CLAUDE.md §8: Gönderi Onay Sistemi akışı
 *
 * Neden onWrite yerine onCreate değil:
 * - İstemci `createPost` Firestore'a `status: "draft"` yazar.
 * - `submitPost` CF durumu `pending`e çevirir.
 * - `onCreate` snapshot'ında durum `draft`tır ve `pending` hiç görülmez.
 * - `onWrite` her belge değişiminde çalışır; `before.status !== pending && after.status === pending`
 *   koşuluyla yalnızca bu geçişi yakalar.
 *
 * Risk seviyeleri → aksiyonlar:
 * - NONE / LOW : moderasyon kuyruğuna al (normal akış)
 * - MEDIUM     : isFlagged=true + moderasyon kuyruğuna al
 * - HIGH       : otomatik reject
 * - CRITICAL   : otomatik reject + yönetici bildirimi
 */
export const onPostStatusChanged = functions
  .region("europe-west1")
  .firestore.document("posts/{postId}")
  .onWrite(async (change, context) => {
    const before = change.before.exists ? change.before.data() : null;
    const after = change.after.exists ? change.after.data() : null;
    const postId = context.params.postId;
    const db = admin.firestore();

    if (!after) return; // Silme eventi — işlem yok

    // Yalnızca draft → pending geçişini yakala
    const wasPending = before?.status === "pending";
    const isPending = after.status === "pending";
    if (wasPending || !isPending) return;

    // Yasaklı kelime listesini ortak yardımcıdan yükle
    const bannedTerms = await loadBannedTerms(db);

    const textToAnalyze = `${after.title ?? ""} ${after.content ?? ""}`;
    const result = analyzeContent(textToAnalyze, bannedTerms);

    functions.logger.info(`İçerik filtresi sonucu: ${postId}`, {
      riskLevel: result.riskLevel,
      reasons: result.reasons,
      flagged: result.flagged,
      blocked: result.blocked,
    });

    const now = admin.firestore.FieldValue.serverTimestamp();

    // CRITICAL veya HIGH: otomatik reddet
    if (result.blocked) {
      await change.after.ref.update({
        status: "rejected",
        rejectionReason: `Otomatik filtre: ${result.reasons.join("; ")}`,
        moderatedBy: "system",
        filterRiskLevel: result.riskLevel,
        updatedAt: now,
      });

      functions.logger.warn(`Gönderi otomatik reddedildi (${result.riskLevel}): ${postId}`);

      if (result.riskLevel === "CRITICAL") {
        await db.collection("adminTasks").add({
          type: "critical_content_alert",
          postId,
          authorUid: after.authorUid ?? "",
          reasons: result.reasons,
          riskLevel: result.riskLevel,
          createdAt: now,
          status: "pending",
        });
      }

      if (after.authorUid) {
        await db
          .collection("users")
          .doc(after.authorUid as string)
          .collection("notifications")
          .add({
            type: "post_rejected",
            title: "Gönderiniz Yayımlanamadı",
            body: "Gönderiniz topluluk kurallarına aykırı içerik nedeniyle yayımlanmadı.",
            postId,
            isRead: false,
            createdAt: now,
          });
      }

      return;
    }

    // MEDIUM: flag ekle
    if (result.flagged) {
      await change.after.ref.update({
        isFlagged: true,
        flagReason: result.reasons.join("; "),
        filterRiskLevel: result.riskLevel,
        updatedAt: now,
      });
      functions.logger.info(`Gönderi işaretlendi (${result.riskLevel}): ${postId}`);
    }

    // Moderasyon kuyruğuna ekle
    await db.collection("moderationQueue").doc(postId).set({
      postId,
      type: "post",
      authorUid: after.authorUid ?? "",
      authorUsername: after.authorUsername ?? "",
      title: after.title ?? "",
      content: after.content ?? "",
      postType: after.postType ?? "",
      category: after.category ?? "",
      isAnonymous: after.isAnonymous ?? false,
      imageUrls: after.imageUrls ?? [],
      status: "pending",
      isFlagged: result.flagged,
      flagReason: result.flagged ? result.reasons.join("; ") : null,
      filterRiskLevel: result.riskLevel,
      createdAt: now,
      updatedAt: now,
    });

    functions.logger.info(`Gönderi moderasyon kuyruğuna alındı: ${postId}`);
  });