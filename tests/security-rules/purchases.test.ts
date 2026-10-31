/**
 * purchases.test.ts — Firestore Güvenlik Kuralları: /purchases/{uid}
 *
 * CLAUDE.md §18: Satın alma hakları istemciden değiştirilemez.
 * Yalnızca validatePurchaseToken Cloud Function yazabilir.
 * Kullanıcı yalnızca kendi satın alma kaydını okuyabilir.
 * docs/15-security-rules-approach.md — purchases koleksiyonu güvenlik kuralları.
 *
 * KURAL (firestore.rules):
 *   match /purchases/{uid}
 *     allow read:  if isOwner(uid);   // yalnızca sahip okuyabilir
 *     allow write: if false;          // yalnızca Cloud Functions
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

const OWNER_UID = "purchase_owner_001";
const OTHER_UID = "other_user_002";
const MODERATOR_UID = "moderator_001";
const ADMIN_UID = "admin_001";

const makePurchaseData = (overrides: Record<string, unknown> = {}) => ({
  uid: OWNER_UID,
  adsRemoved: true,
  purchaseToken: "GPA.xxx",
  productId: "remove_ads",
  purchasedAt: new Date(),
  validatedAt: new Date(),
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

  // Cloud Functions Admin SDK simülasyonu ile satın alma kaydı oluştur
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, "purchases", OWNER_UID), makePurchaseData());
  });
});

// ============================================================
// OKUMA TESTLERİ — read: if isOwner(uid)
// ============================================================

describe("Satın alma okuma kuralları", () => {
  test("Kimliği doğrulanmamış kullanıcı satın alma kaydını okuyamaz", async () => {
    const ctx = unauthenticatedContext();
    const db = ctx.firestore();
    await assertFails(getDoc(doc(db, "purchases", OWNER_UID)));
  });

  test("Satın alma sahibi kendi kaydını okuyabilir", async () => {
    // Kural: isOwner(uid) → isAuthenticated() && request.auth.uid == uid
    const ctx = authenticatedContext(OWNER_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "purchases", OWNER_UID)));
  });

  test("Başka kullanıcı satın alma kaydını okuyamaz", async () => {
    // OWNER_UID != OTHER_UID → isOwner() false
    const ctx = authenticatedContext(OTHER_UID);
    const db = ctx.firestore();
    await assertFails(getDoc(doc(db, "purchases", OWNER_UID)));
  });

  test("Moderatör başkasının satın alma kaydını okuyamaz", async () => {
    // Kural: isOwner(uid) — moderatör özel kontrolü yoktur.
    const ctx = moderatorContext(MODERATOR_UID);
    const db = ctx.firestore();
    await assertFails(getDoc(doc(db, "purchases", OWNER_UID)));
  });

  test("Admin başkasının satın alma kaydını okuyamaz (isOwner kuralı)", async () => {
    // Kural: isOwner(uid) — isAdmin() kontrolü yoktur bu koleksiyonda.
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertFails(getDoc(doc(db, "purchases", OWNER_UID)));
  });

  test("Engellenmiş kullanıcı kendi satın alma kaydını okuyabilir", async () => {
    // isOwner(uid): isAuthenticated() + uid eşleşmesi — ban kontrolü yoktur.
    const ctx = bannedUserContext(OWNER_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "purchases", OWNER_UID)));
  });
});

// ============================================================
// YAZMA TESTLERİ — write: if false
// ============================================================

describe("Satın alma yazma kuralları (write: if false)", () => {
  test("Satın alma sahibi kendi kaydını istemciden güncelleyemez", async () => {
    // CLAUDE.md §18: Satın alma hakları istemciden değiştirilemez.
    const ctx = authenticatedContext(OWNER_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "purchases", OWNER_UID), {
        adsRemoved: false,
      })
    );
  });

  test("Başka kullanıcı satın alma kaydını güncelleyemez", async () => {
    const ctx = authenticatedContext(OTHER_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "purchases", OWNER_UID), {
        adsRemoved: false,
      })
    );
  });

  test("Moderatör satın alma kaydını güncelleyemez", async () => {
    const ctx = moderatorContext(MODERATOR_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "purchases", OWNER_UID), {
        adsRemoved: false,
      })
    );
  });

  test("Admin istemciden satın alma kaydını güncelleyemez (write: if false)", async () => {
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "purchases", OWNER_UID), {
        adsRemoved: false,
      })
    );
  });

  test("Hiçbir kullanıcı yeni satın alma kaydı oluşturamaz", async () => {
    const ctx = authenticatedContext(OTHER_UID);
    const db = ctx.firestore();
    await assertFails(
      setDoc(doc(db, "purchases", OTHER_UID), makePurchaseData({ uid: OTHER_UID }))
    );
  });

  test("Hiçbir kullanıcı satın alma kaydını silemez", async () => {
    const ctx = authenticatedContext(OWNER_UID);
    const db = ctx.firestore();
    await assertFails(deleteDoc(doc(db, "purchases", OWNER_UID)));
  });

  test("Kimliği doğrulanmamış kullanıcı satın alma kaydı oluşturamaz", async () => {
    const ctx = unauthenticatedContext();
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "purchases", "new_uid"),
        makePurchaseData({ uid: "new_uid" })
      )
    );
  });
});