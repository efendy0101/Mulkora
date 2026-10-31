/**
 * reports.test.ts — Firestore Güvenlik Kuralları: /reports/{reportId}
 *
 * CLAUDE.md §11: Şikayetler — yalnızca createReport Cloud Function üzerinden
 * oluşturulur. Mükerrer şikayet kontrolü CF tarafında yapılır.
 * docs/15-security-rules-approach.md — reports koleksiyonu güvenlik kuralları.
 *
 * KURAL (firestore.rules):
 *   match /reports/{reportId}
 *     allow read:   if isModerator() ||
 *                      (isAuthenticated() && resource.data.reporterUid == request.auth.uid)
 *     allow create: if false;               // Cloud Functions
 *     allow update: if isModerator();
 *     allow delete: if isAdmin();
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

const REPORTER_UID = "reporter_user_001";
const OTHER_UID = "other_user_002";
const MODERATOR_UID = "moderator_001";
const ADMIN_UID = "admin_001";
const REPORT_ID = "report_test_001";
const TARGET_POST_ID = "post_001";

const makeReportData = (overrides: Record<string, unknown> = {}) => ({
  reporterUid: REPORTER_UID,
  targetType: "post",
  targetId: TARGET_POST_ID,
  reason: "spam",
  status: "pending",
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

  // Cloud Functions Admin SDK simülasyonu ile rapor oluştur
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, "reports", REPORT_ID), makeReportData());
  });
});

// ============================================================
// OKUMA TESTLERİ
// ============================================================

describe("Rapor okuma kuralları", () => {
  test("Kimliği doğrulanmamış kullanıcı rapor okuyamaz", async () => {
    const ctx = unauthenticatedContext();
    const db = ctx.firestore();
    await assertFails(getDoc(doc(db, "reports", REPORT_ID)));
  });

  test("Raporu oluşturan kullanıcı kendi raporunu okuyabilir", async () => {
    // Kural: isAuthenticated() && resource.data.reporterUid == request.auth.uid
    const ctx = authenticatedContext(REPORTER_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "reports", REPORT_ID)));
  });

  test("Başka kullanıcı raporu okuyamaz", async () => {
    // OTHER_UID != REPORTER_UID ve moderatör değil
    const ctx = authenticatedContext(OTHER_UID);
    const db = ctx.firestore();
    await assertFails(getDoc(doc(db, "reports", REPORT_ID)));
  });

  test("Moderatör tüm raporları okuyabilir", async () => {
    // Kural: isModerator() — moderatör her raporu okuyabilir
    const ctx = moderatorContext(MODERATOR_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "reports", REPORT_ID)));
  });

  test("Admin tüm raporları okuyabilir", async () => {
    // Admin, isModerator() koşulunu karşılar (role == 'admin')
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "reports", REPORT_ID)));
  });

  test("Engellenmiş kullanıcı kendi raporunu okuyabilir", async () => {
    // Kural reporterUid eşleşmesi + isAuthenticated() — ban kontrolü yoktur.
    const ctx = bannedUserContext(REPORTER_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "reports", REPORT_ID)));
  });
});

// ============================================================
// OLUŞTURMA TESTLERİ — create: if false
// ============================================================

describe("Rapor oluşturma kuralları (create: if false)", () => {
  test("Kimliği doğrulanmış kullanıcı istemciden rapor oluşturamaz", async () => {
    // CLAUDE.md §11: createReport Cloud Function üzerinden çağrılmalı.
    const ctx = authenticatedContext(OTHER_UID);
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "reports", "new_report_999"),
        makeReportData({ reporterUid: OTHER_UID })
      )
    );
  });

  test("Kimliği doğrulanmamış kullanıcı rapor oluşturamaz", async () => {
    const ctx = unauthenticatedContext();
    const db = ctx.firestore();
    await assertFails(
      setDoc(doc(db, "reports", "new_report_888"), makeReportData())
    );
  });

  test("Moderatör istemciden rapor oluşturamaz", async () => {
    const ctx = moderatorContext(MODERATOR_UID);
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "reports", "new_report_777"),
        makeReportData({ reporterUid: MODERATOR_UID })
      )
    );
  });

  test("Admin istemciden rapor oluşturamaz (create: if false)", async () => {
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "reports", "new_report_666"),
        makeReportData({ reporterUid: ADMIN_UID })
      )
    );
  });
});

// ============================================================
// GÜNCELLEME TESTLERİ — update: if isModerator()
// ============================================================

describe("Rapor güncelleme kuralları", () => {
  test("Moderatör raporu güncelleyebilir", async () => {
    // Moderatörler rapor durumunu güncelleyebilir (örn. resolved, dismissed)
    const ctx = moderatorContext(MODERATOR_UID);
    const db = ctx.firestore();
    await assertSucceeds(
      updateDoc(doc(db, "reports", REPORT_ID), {
        status: "resolved",
        updatedAt: new Date(),
      })
    );
  });

  test("Admin raporu güncelleyebilir", async () => {
    // Admin isModerator() koşulunu karşılar
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertSucceeds(
      updateDoc(doc(db, "reports", REPORT_ID), {
        status: "dismissed",
        updatedAt: new Date(),
      })
    );
  });

  test("Raporu oluşturan kullanıcı raporu güncelleyemez", async () => {
    // Kural: update: if isModerator() — reporter güncelleyemez
    const ctx = authenticatedContext(REPORTER_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "reports", REPORT_ID), {
        reason: "Değiştirilmiş sebep",
      })
    );
  });

  test("Başka kullanıcı raporu güncelleyemez", async () => {
    const ctx = authenticatedContext(OTHER_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "reports", REPORT_ID), {
        status: "resolved",
      })
    );
  });

  test("Kimliği doğrulanmamış kullanıcı raporu güncelleyemez", async () => {
    const ctx = unauthenticatedContext();
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "reports", REPORT_ID), {
        status: "resolved",
      })
    );
  });
});

// ============================================================
// SİLME TESTLERİ — delete: if isAdmin()
// ============================================================

describe("Rapor silme kuralları", () => {
  test("Admin raporu silebilir", async () => {
    // Kural: allow delete: if isAdmin()
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertSucceeds(deleteDoc(doc(db, "reports", REPORT_ID)));
  });

  test("Moderatör raporu silemez", async () => {
    // Moderatör, isAdmin() koşulunu karşılamaz
    const ctx = moderatorContext(MODERATOR_UID);
    const db = ctx.firestore();
    await assertFails(deleteDoc(doc(db, "reports", REPORT_ID)));
  });

  test("Raporu oluşturan kullanıcı kendi raporunu silemez", async () => {
    const ctx = authenticatedContext(REPORTER_UID);
    const db = ctx.firestore();
    await assertFails(deleteDoc(doc(db, "reports", REPORT_ID)));
  });

  test("Başka kullanıcı raporu silemez", async () => {
    const ctx = authenticatedContext(OTHER_UID);
    const db = ctx.firestore();
    await assertFails(deleteDoc(doc(db, "reports", REPORT_ID)));
  });

  test("Kimliği doğrulanmamış kullanıcı raporu silemez", async () => {
    const ctx = unauthenticatedContext();
    const db = ctx.firestore();
    await assertFails(deleteDoc(doc(db, "reports", REPORT_ID)));
  });
});