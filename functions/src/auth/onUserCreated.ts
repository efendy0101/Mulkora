import * as admin from "../firebaseAdmin";
import * as functions from "firebase-functions/v1";

const db = admin.firestore();

/**
 * Yeni kullanıcı kayıt olduğunda tetiklenir.
 * CLAUDE.md Bölüm 2: Kimlik Doğrulama
 * docs/14-cloud-functions-architecture.md
 *
 * Yapılan işlemler:
 * 1. Firestore'da users/{uid} belgesi oluşturur
 * 2. Varsayılan rol "tenant" olarak atanır
 * 3. Kullanıcı adı e-postadan türetilir (uid prefix)
 */
export const onUserCreated = functions
  .region("europe-west1")
  .auth.user()
  .onCreate(async (user) => {
    const { uid, email, displayName, photoURL } = user;

    // Kullanıcı adı: email prefix + uid son 4 karakter
    const emailPrefix = (email ?? "user")
      .split("@")[0]
      .toLowerCase()
      .replace(/[^a-z0-9_]/g, "");
    const usernameBase = emailPrefix.slice(0, 12);
    const usernameSuffix = uid.slice(-4).toLowerCase();
    const username = `${usernameBase}_${usernameSuffix}`;

    const now = admin.firestore.FieldValue.serverTimestamp();

    const userDoc = {
      uid,
      username,
      displayName: displayName ?? emailPrefix,
      email: email ?? "",
      photoUrl: photoURL ?? null,
      bio: null,
      role: "tenant",                   // Varsayılan rol
      isEmailVerified: user.emailVerified,
      isBlocked: false,
      adsRemoved: false,
      postCount: 0,
      referenceCount: 0,
      averageReferenceRating: 0.0,
      ratingCount: 0,
      city: null,
      links: null,
      fcmTokens: [],
      createdAt: now,
      updatedAt: now,
    };

    try {
      await db.collection("users").doc(uid).create(userDoc);
      functions.logger.info(`Kullanıcı profili oluşturuldu: ${uid}`);
    } catch (error) {
      functions.logger.error(`Kullanıcı profili oluşturulamadı: ${uid}`, error);
    }
  });