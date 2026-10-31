/**
 * blockedUsers.test.ts — Firestore Güvenlik Kuralları: /blockedUsers/{blockId}
 *
 * CLAUDE.md §: Kullanıcı engelleme sistemi — blockUser/unblockUser Cloud Functions.
 * blockId formatı: "{blockerUid}_{blockedUid}"
 * docs/15-security-rules-approach.md — blockedUsers koleksiyonu güvenlik kuralları.
 *
 * KURAL (firestore.rules):
 *   match /blockedUsers/{blockId}
 *     allow read:  if isAuthenticated() &&
 *                     request.auth.uid == resource.data.blockerUid;
 *     allow write: if false;  // blockUser/unblockUser Cloud Functions
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

const BLOCKER_UID = "blocker_user_001";
const BLOCKED_UID = "blocked_user_002";
const OTHER_UID = "other_user_003";
const MODERATOR_UID = "moderator_001";
const ADMIN_UID = "admin_001";

// blockId = "{blockerUid}_{blockedUid}" formatı
const BLOCK_ID = `${BLOCKER_UID}_${BLOCKED_UID}`;

const makeBlockData = (overrides: Record<string, unknown> = {}) => ({
  blockerUid: BLOCKER_UID,
  blockedUid: BLOCKED_UID,
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

  // Cloud Functions Admin SDK simülasyonu ile engelleme kaydı oluştur
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, "blockedUsers", BLOCK_ID), makeBlockData());
  });
});

// ============================================================
// OKUMA TESTLERİ — read: request.auth.uid == resource.data.blockerUid
// ============================================================

describe("Engelleme kaydı okuma kuralları", () => {
  test("Kimliği doğrulanmamış kullanıcı engelleme kaydını okuyamaz", async () => {
    const ctx = unauthenticatedContext();
    const db = ctx.firestore();
    await assertFails(getDoc(doc(db, "blockedUsers", BLOCK_ID)));
  });

  test("Engelleyen kullanıcı kendi engelleme kaydını okuyabilir", async () => {
    // Kural: request.auth.uid == resource.data.blockerUid
    const ctx = authenticatedContext(BLOCKER_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "blockedUsers", BLOCK_ID)));
  });

  test("Engellenen kullanıcı kendi engellenme kaydını okuyamaz", async () => {
    // Kural yalnızca blockerUid'e izin verir, blockedUid'e değil
    const ctx = authenticatedContext(BLOCKED_UID);
    const db = ctx.firestore();
    await assertFails(getDoc(doc(db, "blockedUsers", BLOCK_ID)));
  });

  test("Başka kullanıcı engelleme kaydını okuyamaz", async () => {
    // OTHER_UID ne blocker ne de blocked
    const ctx = authenticatedContext(OTHER_UID);
    const db = ctx.firestore();
    await assertFails(getDoc(doc(db, "blockedUsers", BLOCK_ID)));
  });

  test("Moderatör başkasının engelleme kaydını okuyamaz", async () => {
    // Kural: blockerUid eşleşmesi — moderatör özel kontrolü yoktur.
    const ctx = moderatorContext(MODERATOR_UID);
    const db = ctx.firestore();
    await assertFails(getDoc(doc(db, "blockedUsers", BLOCK_ID)));
  });

  test("Admin başkasının engelleme kaydını okuyamaz (blockerUid kuralı)", async () => {
    // Kural: blockerUid eşleşmesi — isAdmin() kontrolü yoktur bu koleksiyonda.
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertFails(getDoc(doc(db, "blockedUsers", BLOCK_ID)));
  });

  test("Engellenmiş kullanıcı kendi engelleme kaydını okuyabilir", async () => {
    // Engelleyen kullanıcı ban'lı bile olsa kendi blocker kaydını okuyabilir
    const ctx = bannedUserContext(BLOCKER_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "blockedUsers", BLOCK_ID)));
  });
});

// ============================================================
// YAZMA TESTLERİ — write: if false
// ============================================================

describe("Engelleme kaydı yazma kuralları (write: if false)", () => {
  test("Engelleyen kullanıcı istemciden yeni engelleme oluşturamaz", async () => {
    // CLAUDE.md: blockUser Cloud Function üzerinden çağrılmalı
    const ctx = authenticatedContext(BLOCKER_UID);
    const db = ctx.firestore();
    const newBlockId = `${BLOCKER_UID}_${OTHER_UID}`;
    await assertFails(
      setDoc(
        doc(db, "blockedUsers", newBlockId),
        makeBlockData({ blockedUid: OTHER_UID })
      )
    );
  });

  test("Başka kullanıcı engelleme kaydı oluşturamaz", async () => {
    const ctx = authenticatedContext(OTHER_UID);
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "blockedUsers", `${OTHER_UID}_${BLOCKED_UID}`),
        makeBlockData({ blockerUid: OTHER_UID })
      )
    );
  });

  test("Kimliği doğrulanmamış kullanıcı engelleme kaydı oluşturamaz", async () => {
    const ctx = unauthenticatedContext();
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "blockedUsers", "anon_block"),
        makeBlockData()
      )
    );
  });

  test("Moderatör engelleme kaydı oluşturamaz (write: if false)", async () => {
    const ctx = moderatorContext(MODERATOR_UID);
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "blockedUsers", `${MODERATOR_UID}_${OTHER_UID}`),
        makeBlockData({ blockerUid: MODERATOR_UID, blockedUid: OTHER_UID })
      )
    );
  });

  test("Admin istemciden engelleme kaydı oluşturamaz (write: if false)", async () => {
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "blockedUsers", `${ADMIN_UID}_${OTHER_UID}`),
        makeBlockData({ blockerUid: ADMIN_UID, blockedUid: OTHER_UID })
      )
    );
  });

  test("Hiçbir kullanıcı engelleme kaydını güncelleyemez", async () => {
    const ctx = authenticatedContext(BLOCKER_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "blockedUsers", BLOCK_ID), {
        updatedAt: new Date(),
      })
    );
  });

  test("Engelleyen kullanıcı kendi kaydını istemciden silemez", async () => {
    // Engeli kaldırmak için unblockUser Cloud Function kullanılmalı
    const ctx = authenticatedContext(BLOCKER_UID);
    const db = ctx.firestore();
    await assertFails(deleteDoc(doc(db, "blockedUsers", BLOCK_ID)));
  });

  test("Admin istemciden engelleme kaydını silemez (write: if false)", async () => {
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertFails(deleteDoc(doc(db, "blockedUsers", BLOCK_ID)));
  });
});