import * as admin from "../firebaseAdmin";
import * as functions from "firebase-functions/v1";

const db = admin.firestore();

/**
 * Kullanıcı adını atomik olarak rezerve eder ve kullanıcı profiline kaydeder.
 *
 * CF-AUTH-003: docs/14-cloud-functions-architecture.md
 * CLAUDE.md §4: Kullanıcı adı benzersizliği backend üzerinden güvenli biçimde kontrol edilmeli.
 * CLAUDE.md §26: /usernames koleksiyonu istemci tarafından yazılamaz — sadece Cloud Functions.
 *
 * Kurallar:
 * - Kullanıcı adı formatı checkUsername ile aynı kuralları uygular.
 * - Transaction ile race condition güvenli atomik rezervasyon yapılır.
 * - Önceki kullanıcı adı varsa serbest bırakılır (değiştirme senaryosu).
 * - Kullanıcı başka birinin adını üzerine yazamaz.
 */
export const reserveUsername = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Bu işlem için giriş yapılmış olmalıdır."
      );
    }

    const uid = context.auth.uid;
    const { username } = data;

    if (!username || typeof username !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Geçerli bir kullanıcı adı gereklidir."
      );
    }

    const normalized = username.toLowerCase().trim();

    // Format doğrulama (checkUsername ile aynı kural seti)
    const usernameRegex = /^[a-z0-9][a-z0-9._]{1,18}[a-z0-9]$|^[a-z0-9]{3,20}$/;
    if (!usernameRegex.test(normalized)) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Kullanıcı adı 3–20 karakter olmalı; harf, rakam, nokta ve alt çizgi içerebilir."
      );
    }

    // Yasaklı kelime kontrolü
    const reservedNames = ["admin", "moderator", "mulkora", "support", "help"];
    if (reservedNames.some((r) => normalized.includes(r))) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Bu kullanıcı adı kullanılamaz."
      );
    }

    const usernameRef = db.collection("usernames").doc(normalized);
    const userRef = db.collection("users").doc(uid);

    try {
      await db.runTransaction(async (tx) => {
        const [usernameSnap, userSnap] = await Promise.all([
          tx.get(usernameRef),
          tx.get(userRef),
        ]);

        // Başka bir kullanıcı bu adı almış mı?
        if (usernameSnap.exists) {
          const existingUid = usernameSnap.data()?.uid;
          if (existingUid !== uid) {
            throw new functions.https.HttpsError(
              "already-exists",
              "Bu kullanıcı adı zaten kullanımda."
            );
          }
          // Aynı kullanıcı aynı adı tekrar alıyor — idempotent, başarılı say
          return;
        }

        const previousUsername: string | undefined = userSnap.data()?.username;

        // Eski kullanıcı adını serbest bırak
        if (previousUsername && previousUsername !== normalized) {
          const oldRef = db.collection("usernames").doc(previousUsername);
          tx.delete(oldRef);
        }

        // Yeni kullanıcı adını rezerve et
        tx.set(usernameRef, {
          uid,
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        // Kullanıcı belgesini güncelle
        tx.update(userRef, {
          username: normalized,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
      });
    } catch (err) {
      if (err instanceof functions.https.HttpsError) throw err;
      functions.logger.error(`reserveUsername transaction hatası: ${uid}`, err);
      throw new functions.https.HttpsError(
        "internal",
        "Kullanıcı adı kaydedilirken bir hata oluştu. Lütfen tekrar deneyin."
      );
    }

    functions.logger.info(`Kullanıcı adı rezerve edildi: ${normalized} → ${uid}`);
    return { success: true, username: normalized };
  });