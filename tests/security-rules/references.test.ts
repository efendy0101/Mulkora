/**
 * references.test.ts — Firestore Güvenlik Kuralları: /references/{referenceId}
 *
 * CLAUDE.md §12: Referans Sistemi
 * CLAUDE.md §26: Firestore ve Storage Güvenliği
 * docs/16-reference-system-state-machine.md
 *
 * KURALAR (firestore.rules):
 *   /references/{referenceId}
 *     allow read:  if isParty(resource) || isModerator() || isAdmin();
 *     allow write: if false;  // Tüm işlemler Cloud Function üzerinden
 *
 *   isParty = fromUid veya toUid eşleşiyor
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

const FROM_UID = "user_from_001";   // Referans veren
const TO_UID = "user_to_002";       // Referans alan
const OTHER_UID = "user_other_003"; // Taraf olmayan
const MODERATOR_UID = "moderator_001";
const ADMIN_UID = "admin_001";

const REFERENCE_ID = "ref_001";

const makeReferenceData = (
  status: string,
  overrides: Record<string, unknown> = {}
) => ({
  fromUid: FROM_UID,
  fromUsername: "user_from",
  toUid: TO_UID,
  toUsername: "user_to",
  relationship: "tenant_to_landlord",
  content: "Çok iyi bir ev sahibiydi.",
  rating: 4,
  status,
  isFlagged: false,
  flagReason: null,
  filterRiskLevel: null,
  moderatorNote: null,
  createdAt: new Date(),
  updatedAt: new Date(),
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

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    // Onaylanmış referans
    await setDoc(
      doc(db, "references", REFERENCE_ID),
      makeReferenceData("approved")
    );
    // Bekleyen referans
    await setDoc(
      doc(db, "references", "ref_pending_001"),
      makeReferenceData("pending_approval")
    );
    // İtirazda referans
    await setDoc(
      doc(db, "references", "ref_disputed_001"),
      makeReferenceData("disputed")
    );
  });
});

// ============================================================
// OKUMA TESTLERİ — isParty || isModerator() || isAdmin()
// ============================================================

describe("references: okuma kuralları", () => {
  test("Kimliği doğrulanmamış kullanıcı referans okuyamaz", async () => {
    const ctx = unauthenticatedContext();
    const db = ctx.firestore();
    await assertFails(getDoc(doc(db, "references", REFERENCE_ID)));
  });

  test("Referans veren (fromUid) referansı okuyabilir", async () => {
    const ctx = authenticatedContext(FROM_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "references", REFERENCE_ID)));
  });

  test("Referans alan (toUid) referansı okuyabilir", async () => {
    const ctx = authenticatedContext(TO_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "references", REFERENCE_ID)));
  });

  test("Taraf olmayan kullanıcı referansı okuyamaz", async () => {
    const ctx = authenticatedContext(OTHER_UID);
    const db = ctx.firestore();
    await assertFails(getDoc(doc(db, "references", REFERENCE_ID)));
  });

  test("Moderatör referansı okuyabilir", async () => {
    const ctx = moderatorContext(MODERATOR_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "references", REFERENCE_ID)));
  });

  test("Admin referansı okuyabilir", async () => {
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "references", REFERENCE_ID)));
  });

  test("Referans veren bekleyen referansı okuyabilir", async () => {
    const ctx = authenticatedContext(FROM_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "references", "ref_pending_001")));
  });

  test("Taraf olmayan kullanıcı bekleyen referansı okuyamaz", async () => {
    const ctx = authenticatedContext(OTHER_UID);
    const db = ctx.firestore();
    await assertFails(getDoc(doc(db, "references", "ref_pending_001")));
  });

  test("Moderatör itirazda olan referansı okuyabilir", async () => {
    const ctx = moderatorContext(MODERATOR_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "references", "ref_disputed_001")));
  });

  test("Engellenen kullanıcı taraf olduğu referansı okuyabilir (isParty)", async () => {
    const ctx = bannedUserContext(FROM_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "references", REFERENCE_ID)));
  });
});

// ============================================================
// OLUŞTURMA TESTLERİ — write: if false
// ============================================================

describe("references: oluşturma kuralları (write: if false)", () => {
  test("Kimliği doğrulanmış kullanıcı istemciden referans oluşturamaz", async () => {
    const ctx = authenticatedContext(FROM_UID);
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "references", "new_ref_xxx"),
        makeReferenceData("draft")
      )
    );
  });

  test("Referans alan kendi adına referans oluşturamaz (write: if false)", async () => {
    // Başka biri adına referans oluşturma girişimi
    const ctx = authenticatedContext(FROM_UID);
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "references", "fake_ref_xxx"),
        makeReferenceData("draft", { fromUid: OTHER_UID, toUid: FROM_UID })
      )
    );
  });

  test("Admin istemciden referans oluşturamaz (write: if false)", async () => {
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "references", "admin_ref_xxx"),
        makeReferenceData("approved")
      )
    );
  });

  test("Kimliği doğrulanmamış kullanıcı referans oluşturamaz", async () => {
    const ctx = unauthenticatedContext();
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "references", "anon_ref_xxx"),
        makeReferenceData("draft")
      )
    );
  });
});

// ============================================================
// GÜNCELLEME TESTLERİ — write: if false
// ============================================================

describe("references: güncelleme kuralları (write: if false)", () => {
  test("Referans veren istemciden referansı güncelleyemez", async () => {
    const ctx = authenticatedContext(FROM_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "references", REFERENCE_ID), {
        content: "Değiştirilmiş içerik.",
      })
    );
  });

  test("Referans alan istemciden referansı güncelleyemez", async () => {
    const ctx = authenticatedContext(TO_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "references", REFERENCE_ID), {
        rating: 1,
      })
    );
  });

  test("Moderatör istemciden referans durumunu değiştiremez (write: if false)", async () => {
    const ctx = moderatorContext(MODERATOR_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "references", "ref_pending_001"), {
        status: "approved",
      })
    );
  });

  test("Admin istemciden referansı güncelleyemez (write: if false)", async () => {
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "references", REFERENCE_ID), {
        status: "removed",
      })
    );
  });

  test("Başka kullanıcı adına referans veremez (write: if false)", async () => {
    // CLAUDE.md §32: Başka kullanıcı adına referans verme girişimi
    const ctx = authenticatedContext(OTHER_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "references", REFERENCE_ID), {
        fromUid: OTHER_UID,
        content: "Manipüle edilmiş referans.",
      })
    );
  });
});

// ============================================================
// SİLME TESTLERİ — write: if false
// ============================================================

describe("references: silme kuralları (write: if false)", () => {
  test("Referans veren kendi referansını silemez", async () => {
    const ctx = authenticatedContext(FROM_UID);
    const db = ctx.firestore();
    await assertFails(deleteDoc(doc(db, "references", REFERENCE_ID)));
  });

  test("Admin istemciden referans silemez (write: if false)", async () => {
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertFails(deleteDoc(doc(db, "references", REFERENCE_ID)));
  });

  test("Moderatör referansı silemez (write: if false)", async () => {
    const ctx = moderatorContext(MODERATOR_UID);
    const db = ctx.firestore();
    await assertFails(deleteDoc(doc(db, "references", REFERENCE_ID)));
  });
});