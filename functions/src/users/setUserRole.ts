import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";

/**
 * Kullanıcıya moderatör veya admin rolü atar ya da rolü kaldırır.
 *
 * CLAUDE.md §3.5: Yönetici moderatör atayabilmelidir.
 * CLAUDE.md §25: Yönetici rolü doğrulama backend üzerinden yapılmalıdır.
 * CLAUDE.md §4: Kullanıcı kendi rolünü değiştiremez.
 *
 * Yalnızca role='admin' custom claim'ine sahip kullanıcılar çağırabilir.
 */
export const setUserRole = onCall(
  { region: "europe-west1" },
  async (request) => {
    // Kimlik doğrulama kontrolü
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Kimlik doğrulama gerekli.");
    }

    // Admin rolü kontrolü
    const callerClaims = request.auth.token ?? {};
    if (callerClaims["role"] !== "admin") {
      throw new HttpsError(
        "permission-denied",
        "Bu işlem yalnızca adminler tarafından yapılabilir."
      );
    }

    const { targetUid, role } = request.data as {
      targetUid: string;
      role: "admin" | "moderator" | "user";
    };

    if (!targetUid || typeof targetUid !== "string") {
      throw new HttpsError("invalid-argument", "targetUid gerekli.");
    }

    const validRoles = ["admin", "moderator", "user"];
    if (!validRoles.includes(role)) {
      throw new HttpsError(
        "invalid-argument",
        "Geçersiz rol. Geçerli değerler: admin, moderator, user."
      );
    }

    // Admin kendi rolünü değiştiremez
    if (targetUid === request.auth.uid) {
      throw new HttpsError(
        "failed-precondition",
        "Kendi rolünüzü değiştiremezsiniz."
      );
    }

    const db = admin.firestore();
    const now = admin.firestore.FieldValue.serverTimestamp();

    // Hedef kullanıcının mevcut durumunu al
    const targetUserRecord = await admin.auth().getUser(targetUid);
    const previousRole =
      (targetUserRecord.customClaims ?? {})["role"] ?? "user";

    // Firebase Auth custom claim güncelle
    const newClaims =
      role === "user" ? {} : { role };
    await admin.auth().setCustomUserClaims(targetUid, newClaims);

    // Firestore kullanıcı belgesini güncelle
    await db.collection("users").doc(targetUid).update({
      role,
      updatedAt: now,
    });

    // Denetim kaydı oluştur
    await db.collection("auditLogs").add({
      adminUid: request.auth.uid,
      action: "set_user_role",
      targetType: "user",
      targetId: targetUid,
      previousState: { role: previousRole },
      newState: { role },
      description:
        role === "user"
          ? "Kullanıcı rolü kaldırıldı (normal kullanıcı)"
          : `Kullanıcıya "${role}" rolü atandı`,
      createdAt: now,
    });

    return { success: true, targetUid, previousRole, newRole: role };
  }
);
