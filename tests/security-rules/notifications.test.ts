/**
 * notifications.test.ts — Firestore Güvenlik Kuralları: /users/{uid}/notifications/{notifId}
 *
 * CLAUDE.md §19: Bildirimler — her kullanıcı yalnızca kendi bildirimlerini
 * okuyabilir ve okundu olarak işaretleyebilir. Bildirimler Cloud Functions
 * Admin SDK ile oluşturulur (istemci tarafı create yasak).
 * docs/15-security-rules-approach.md — notifications güvenlik kuralları.
 * docs/19-notification-system.md — bildirim sistemi tasarımı.
 *
 * KURAL (firestore.rules):
 *   match /users/{uid}/notifications/{notifId}
 *     allow read:   if isOwner(uid);                   // yalnızca sahip
 *     allow create: if false;                           // sadece CF Admin SDK
 *     allow update: if isOwner(uid) && affectedKeys = ['isRead','updatedAt']
 *     allow delete: if isOwner(uid);                   // sahip silebilir
 */

import {
  RulesTestEnvironment,
  assertFails,
  assertSucceeds,
} from "@firebase/rules-unit-testing";
import {
  doc,
  getDoc,
  setDoc,
  updateDoc,
  deleteDoc,
} from "firebase/firestore";
import {
  setupTestEnvironment,
  teardownTestEnvironment,
  clearFirestoreData,
  authenticatedContext,
  unauthenticatedContext,
  adminContext,
  moderatorContext,
  bannedUserContext,
} from "./helpers";

let testEnv: RulesTestEnvironment;

const OWNER_UID = "notification_owner_001";
const OTHER_UID = "other_user_002";
const MODERATOR_UID = "moderator_001";
const ADMIN_UID = "admin_001";
const NOTIF_ID = "notification_test_001";

// Bildirimler /users/{uid}/notifications/{notifId} alt koleksiyonunda saklanır.
const notifPath = (uid: string, notifId: string) =>
  `users/${uid}/notifications/${notifId}`;

const makeNotificationData = (overrides: Record<string, unknown> = {}) => ({
  recipientUid: OWNER_UID,
  type: "comment_on_post",
  title: "Gönderinize yorum yapıldı",
  body: "Biri gönderinize yorum yaptı.",
  isRead: false,
  postId: "post_001",
  actorUid: "actor_user",
  createdAt: new Date(),
  ...overrides,
});

beforeAll(async () => {
  testEnv = await setupTestEnvironment();
});

afterAll(async () => {
  await teardownTestEnvironment();
});

beforeEach(async () => {
  await clearFirestoreData();

  // Cloud Functions Admin SDK simülasyonu — kuralları atlayarak yazar
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(
      doc(db, "users", OWNER_UID, "notifications", NOTIF_ID),
      makeNotificationData()
    );
  });
});

// ============================================================
// OKUMA TESTLERİ
// ============================================================

describe("Bildirim okuma kuralları", () => {
  test("Kimliği doğrulanmamış kullanıcı bildirimi okuyamaz", async () => {
    const ctx = unauthenticatedContext();
    const db = ctx.firestore();
    await assertFails(
      getDoc(doc(db, "users", OWNER_UID, "notifications", NOTIF_ID))
    );
  });

  test("Bildirim sahibi kendi bildirimini okuyabilir", async () => {
    const ctx = authenticatedContext(OWNER_UID);
    const db = ctx.firestore();
    await assertSucceeds(
      getDoc(doc(db, "users", OWNER_UID, "notifications", NOTIF_ID))
    );
  });

  test("Başka kullanıcı bildirimi okuyamaz", async () => {
    // isOwner(uid) → request.auth.uid == uid; OTHER_UID != OWNER_UID
    const ctx = authenticatedContext(OTHER_UID);
    const db = ctx.firestore();
    await assertFails(
      getDoc(doc(db, "users", OWNER_UID, "notifications", NOTIF_ID))
    );
  });

  test("Moderatör başka kullanıcının bildirimini okuyamaz", async () => {
    // Kural: isOwner(uid) — moderatör kontrolü yoktur bu koleksiyonda.
    const ctx = moderatorContext(MODERATOR_UID);
    const db = ctx.firestore();
    await assertFails(
      getDoc(doc(db, "users", OWNER_UID, "notifications", NOTIF_ID))
    );
  });

  test("Admin başka kullanıcının bildirimini doğrudan okuyamaz (isOwner kuralı)", async () => {
    // Kural: isOwner(uid) — isAdmin() kontrolü yoktur bu eşleşmede.
    // Admin, diğer kullanıcıların bildirimlerini yalnızca Admin SDK üzerinden okuyabilir.
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertFails(
      getDoc(doc(db, "users", OWNER_UID, "notifications", NOTIF_ID))
    );
  });

  test("Engellenmiş kullanıcı kendi bildirimini okuyabilir", async () => {
    // isOwner(uid) yalnızca kimlik doğrulaması + uid eşleşmesi kontrol eder.
    // Engelleme kontrolü yoktur → bannedUserContext(OWNER_UID) yine de sahiptir.
    const ctx = bannedUserContext(OWNER_UID);
    const db = ctx.firestore();
    await assertSucceeds(
      getDoc(doc(db, "users", OWNER_UID, "notifications", NOTIF_ID))
    );
  });
});

// ============================================================
// YAZMA TESTLERİ — Yalnızca Cloud Functions Admin SDK oluşturabilir
// ============================================================

describe("Bildirim oluşturma kuralları", () => {
  test("Sahip istemciden kendi bildirimine yazamaz (create: if false)", async () => {
    const ctx = authenticatedContext(OWNER_UID);
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "users", OWNER_UID, "notifications", "new_notif_999"),
        makeNotificationData()
      )
    );
  });

  test("Kimliği doğrulanmamış kullanıcı bildirim oluşturamaz", async () => {
    const ctx = unauthenticatedContext();
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "users", OWNER_UID, "notifications", "new_notif_888"),
        makeNotificationData()
      )
    );
  });

  test("Admin istemciden bildirim oluşturamaz (create: if false)", async () => {
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "users", OWNER_UID, "notifications", "new_notif_777"),
        makeNotificationData()
      )
    );
  });
});

// ============================================================
// GÜNCELLEME TESTLERİ — Sadece isRead (ve updatedAt) alanı güncellenebilir
// ============================================================

describe("Bildirim güncelleme kuralları", () => {
  test("Bildirim sahibi isRead alanını güncelleyebilir", async () => {
    // Kural: affectedKeys().hasOnly(['isRead', 'updatedAt'])
    const ctx = authenticatedContext(OWNER_UID);
    const db = ctx.firestore();
    await assertSucceeds(
      updateDoc(doc(db, "users", OWNER_UID, "notifications", NOTIF_ID), {
        isRead: true,
      })
    );
  });

  test("Başka kullanıcı bildirimi güncelleyemez", async () => {
    const ctx = authenticatedContext(OTHER_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "users", OWNER_UID, "notifications", NOTIF_ID), {
        isRead: true,
      })
    );
  });

  test("Bildirim sahibi korunan alanları (type) değiştiremez", async () => {
    // 'type' alanı affectedKeys hasOnly listesinde değildir.
    const ctx = authenticatedContext(OWNER_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "users", OWNER_UID, "notifications", NOTIF_ID), {
        type: "manipulated_type",
      })
    );
  });

  test("Bildirim sahibi recipientUid değiştiremez", async () => {
    const ctx = authenticatedContext(OWNER_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "users", OWNER_UID, "notifications", NOTIF_ID), {
        recipientUid: OTHER_UID,
      })
    );
  });
});

// ============================================================
// SİLME TESTLERİ
// ============================================================

describe("Bildirim silme kuralları", () => {
  test("Bildirim sahibi kendi bildirimini silebilir", async () => {
    // Kural: allow delete: if isOwner(uid)
    const ctx = authenticatedContext(OWNER_UID);
    const db = ctx.firestore();
    await assertSucceeds(
      deleteDoc(doc(db, "users", OWNER_UID, "notifications", NOTIF_ID))
    );
  });

  test("Başka kullanıcı bildirimi silemez", async () => {
    const ctx = authenticatedContext(OTHER_UID);
    const db = ctx.firestore();
    await assertFails(
      deleteDoc(doc(db, "users", OWNER_UID, "notifications", NOTIF_ID))
    );
  });

  test("Kimliği doğrulanmamış kullanıcı bildirimi silemez", async () => {
    const ctx = unauthenticatedContext();
    const db = ctx.firestore();
    await assertFails(
      deleteDoc(doc(db, "users", OWNER_UID, "notifications", NOTIF_ID))
    );
  });

  test("Admin başkasının bildirimini silemez (isOwner kuralı)", async () => {
    // Kural: allow delete: if isOwner(uid) — admin kontrolü yoktur.
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertFails(
      deleteDoc(doc(db, "users", OWNER_UID, "notifications", NOTIF_ID))
    );
  });
});