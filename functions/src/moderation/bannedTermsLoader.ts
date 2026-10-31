import * as admin from "firebase-admin";
import * as functions from "firebase-functions";

/**
 * Firestore `bannedTerms` koleksiyonundan yasaklı terimleri yükler.
 *
 * CLAUDE.md §12: Yasaklı kelime listesi yönetim panelinden güncellenebilir.
 *
 * Tüm içerik filtreleme fonksiyonları (onPostStatusChanged, editComment, addComment)
 * bu yardımcıyı kullanarak tutarlı bir limite sahip olur.
 *
 * Limit: 1000 — çoğu proje için yeterli, Firestore okuma maliyetini sınırlar.
 * Liste büyürse sayfalandırma eklenebilir.
 */
export async function loadBannedTerms(
  db: admin.firestore.Firestore
): Promise<string[]> {
  try {
    const snap = await db
      .collection("bannedTerms")
      .limit(1000)
      .get();

    return snap.docs
      .map((d) => d.data().term as string)
      .filter(Boolean);
  } catch (err) {
    functions.logger.error("bannedTerms listesi yüklenemedi", err);
    // Liste yüklenemese bile filtrelemeye devam et — boş listeyle
    return [];
  }
}