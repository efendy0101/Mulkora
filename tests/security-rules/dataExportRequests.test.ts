/**
 * dataExportRequests.test.ts — Firestore Güvenlik Kuralları: /dataExportRequests/{requestId}
 *
 * CLAUDE.md §20: Kullanıcının veri indirme talebi oluşturabilmesini sağla.
 * Kullanıcı kendi talebini oluşturabilir ve okuyabilir; Cloud Functions işler.
 * docs/15-security-rules-approach.md — dataExportRequests güvenlik kuralları.
 *
 * KURAL (firestore.rules):
 *   match /dataExportRequests/{requestId}
 *     allow read:   if isAuthenticated() && resource.data.uid == request.auth.uid
 *     allow create: if isAuthenticated() &&
 *                      request.resource.data.uid == request.auth.uid &&
 *                      request.resource.data.status == 'pending'
 *     allow update: if false;  // yalnızca Cloud Functions
 *     allow delete: if false;
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

const REQUESTER_UID = "data_requester_001";
const OTHER_UID = "other_user_002";
const MODERATOR_UID = "moderator_001";
const ADMIN_UID = "admin_001";
const REQUEST_ID = "export_request_001";

const makeExportRequestData = (overrides: Record<string, unknown> = {}) => ({
  uid: REQUESTER_UID,
  status: "pending",
  requestedAt: new Date(),
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

  // Var olan bir talep — örneğin kullanıcı daha önce oluşturmuş
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(
      doc(db, "dataExportRequests", REQUEST_ID),
      makeExportRequestData()
    );
  });
});

// ============================================================
// OKUMA TESTLERİ
// ============================================================

describe("Veri indirme talebi okuma kuralları", () => {
  test("Kimliği doğrulanmamış kullanıcı talebi okuyamaz", async () => {
    const ctx = unauthenticatedContext();
    const db = ctx.firestore();
    await assertFails(getDoc(doc(db, "dataExportRequests", REQUEST_ID)));
  });

  test("Talep sahibi kendi talebini okuyabilir", async () => {
    // Kural: isAuthenticated() && resource.data.uid == request.auth.uid
    const ctx = authenticatedContext(REQUESTER_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "dataExportRequests", REQUEST_ID)));
  });

  test("Başka kullanıcı talebi okuyamaz", async () => {
    // OTHER_UID != REQUESTER_UID → kural reddeder
    const ctx = authenticatedContext(OTHER_UID);
    const db = ctx.firestore();
    await assertFails(getDoc(doc(db, "dataExportRequests", REQUEST_ID)));
  });

  test("Moderatör başkasının talebini okuyamaz", async () => {
    // Kural: uid eşleşmesi — moderatör özel kontrolü yoktur.
    const ctx = moderatorContext(MODERATOR_UID);
    const db = ctx.firestore();
    await assertFails(getDoc(doc(db, "dataExportRequests", REQUEST_ID)));
  });

  test("Admin başkasının talebini okuyamaz (uid eşleşmesi kuralı)", async () => {
    // Kural uid eşleşmesi — isAdmin() kontrolü yoktur bu koleksiyonda.
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertFails(getDoc(doc(db, "dataExportRequests", REQUEST_ID)));
  });
});

// ============================================================
// OLUŞTURMA TESTLERİ — create: uid eşleşmesi + status == 'pending'
// ============================================================

describe("Veri indirme talebi oluşturma kuralları", () => {
  test("Kullanıcı kendi uid ile 'pending' durumunda talep oluşturabilir", async () => {
    // Kural: request.resource.data.uid == request.auth.uid && status == 'pending'
    const ctx = authenticatedContext(REQUESTER_UID);
    const db = ctx.firestore();
    await assertSucceeds(
      setDoc(doc(db, "dataExportRequests", "new_request_001"), {
        uid: REQUESTER_UID,
        status: "pending",
        requestedAt: new Date(),
      })
    );
  });

  test("Kullanıcı başka uid ile talep oluşturamaz", async () => {
    // request.resource.data.uid != request.auth.uid → reddedilir
    const ctx = authenticatedContext(REQUESTER_UID);
    const db = ctx.firestore();
    await assertFails(
      setDoc(doc(db, "dataExportRequests", "new_request_fake"), {
        uid: OTHER_UID, // Farklı uid!
        status: "pending",
        requestedAt: new Date(),
      })
    );
  });

  test("Kullanıcı 'pending' dışı bir status ile talep oluşturamaz", async () => {
    // Kural: request.resource.data.status == 'pending' zorunlu
    const ctx = authenticatedContext(REQUESTER_UID);
    const db = ctx.firestore();
    await assertFails(
      setDoc(doc(db, "dataExportRequests", "new_request_bad_status"), {
        uid: REQUESTER_UID,
        status: "completed", // pending değil!
        requestedAt: new Date(),
      })
    );
  });

  test("Kimliği doğrulanmamış kullanıcı talep oluşturamaz", async () => {
    const ctx = unauthenticatedContext();
    const db = ctx.firestore();
    await assertFails(
      setDoc(doc(db, "dataExportRequests", "anon_request"), {
        uid: "anon",
        status: "pending",
        requestedAt: new Date(),
      })
    );
  });

  test("Engellenmiş kullanıcı kendi uid ile talep oluşturabilir", async () => {
    // Kural create'de ban kontrolü yoktur — isAuthenticated() + uid eşleşmesi yeterli
    const ctx = bannedUserContext(REQUESTER_UID);
    const db = ctx.firestore();
    await assertSucceeds(
      setDoc(doc(db, "dataExportRequests", "banned_request_001"), {
        uid: REQUESTER_UID,
        status: "pending",
        requestedAt: new Date(),
      })
    );
  });
});

// ============================================================
// GÜNCELLEME TESTLERİ — update: if false
// ============================================================

describe("Veri indirme talebi güncelleme kuralları (update: if false)", () => {
  test("Talep sahibi kendi talebini güncelleyemez", async () => {
    // Kural: update: if false — yalnızca CF durumu günceller
    const ctx = authenticatedContext(REQUESTER_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "dataExportRequests", REQUEST_ID), {
        status: "completed",
      })
    );
  });

  test("Moderatör talebi güncelleyemez", async () => {
    const ctx = moderatorContext(MODERATOR_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "dataExportRequests", REQUEST_ID), {
        status: "completed",
      })
    );
  });

  test("Admin talebi güncelleyemez (update: if false)", async () => {
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "dataExportRequests", REQUEST_ID), {
        status: "processing",
      })
    );
  });
});

// ============================================================
// SİLME TESTLERİ — delete: if false
// ============================================================

describe("Veri indirme talebi silme kuralları (delete: if false)", () => {
  test("Talep sahibi kendi talebini silemez", async () => {
    const ctx = authenticatedContext(REQUESTER_UID);
    const db = ctx.firestore();
    await assertFails(deleteDoc(doc(db, "dataExportRequests", REQUEST_ID)));
  });

  test("Moderatör talebi silemez", async () => {
    const ctx = moderatorContext(MODERATOR_UID);
    const db = ctx.firestore();
    await assertFails(deleteDoc(doc(db, "dataExportRequests", REQUEST_ID)));
  });

  test("Admin talebi silemez (delete: if false)", async () => {
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertFails(deleteDoc(doc(db, "dataExportRequests", REQUEST_ID)));
  });

  test("Kimliği doğrulanmamış kullanıcı talebi silemez", async () => {
    const ctx = unauthenticatedContext();
    const db = ctx.firestore();
    await assertFails(deleteDoc(doc(db, "dataExportRequests", REQUEST_ID)));
  });
});