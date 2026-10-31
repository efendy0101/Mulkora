/**
 * postRatings.test.ts — Firestore Güvenlik Kuralları: /postRatings/{ratingId}
 *
 * ratingId formatı: "{postId}_{userId}"
 * docs/15-security-rules-approach.md — postRatings ve commentReactions kuralları.
 *
 * KURAL (firestore.rules):
 *   match /postRatings/{ratingId}
 *     allow read:  if isAuthenticated();
 *     allow write: if false;  // ratePost Cloud Function üzerinden
 */

import {
  RulesTestEnvironment,
  assertFails,
  assertSucceeds,
} from "@firebase/rules-unit-testing";
import {
  doc,
  getDoc,
  getDocs,
  collection,
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

const POST_ID = "post_001";
const AUTHOR_UID = "user_author_001";
const RATER_UID = "user_rater_002";
const OTHER_UID = "user_other_003";
const MODERATOR_UID = "moderator_001";
const ADMIN_UID = "admin_001";

// ratingId = "{postId}_{userId}"
const RATING_ID = `${POST_ID}_${RATER_UID}`;

const makeRatingData = (overrides: Record<string, unknown> = {}) => ({
  postId: POST_ID,
  userId: RATER_UID,
  rating: 4,
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

  // Cloud Functions Admin SDK simülasyonu
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, "postRatings", RATING_ID), makeRatingData());
  });
});

// ============================================================
// OKUMA TESTLERİ — read: if isAuthenticated()
// ============================================================

describe("postRatings okuma kuralları", () => {
  test("Kimliği doğrulanmamış kullanıcı puan belgesi okuyamaz", async () => {
    const ctx = unauthenticatedContext();
    const db = ctx.firestore();
    await assertFails(getDoc(doc(db, "postRatings", RATING_ID)));
  });

  test("Kimliği doğrulanmış kullanıcı puan belgesini okuyabilir", async () => {
    const ctx = authenticatedContext(RATER_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "postRatings", RATING_ID)));
  });

  test("Puan sahibi kendi puan belgesini okuyabilir", async () => {
    const ctx = authenticatedContext(RATER_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "postRatings", RATING_ID)));
  });

  test("Başka kullanıcı da puan belgesini okuyabilir (isAuthenticated)", async () => {
    const ctx = authenticatedContext(OTHER_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "postRatings", RATING_ID)));
  });

  test("Gönderi yazarı puan belgesini okuyabilir", async () => {
    const ctx = authenticatedContext(AUTHOR_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "postRatings", RATING_ID)));
  });

  test("Moderatör puan belgesini okuyabilir", async () => {
    const ctx = moderatorContext(MODERATOR_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "postRatings", RATING_ID)));
  });

  test("Admin puan belgesini okuyabilir", async () => {
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "postRatings", RATING_ID)));
  });

  test("Engellenen kullanıcı da puan belgesini okuyabilir (isAuthenticated)", async () => {
    const ctx = bannedUserContext(OTHER_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "postRatings", RATING_ID)));
  });
});

// ============================================================
// YAZMA TESTLERİ — write: if false
// ============================================================

describe("postRatings yazma kuralları (write: if false)", () => {
  test("Kimliği doğrulanmış kullanıcı istemciden puan oluşturamaz", async () => {
    const ctx = authenticatedContext(RATER_UID);
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "postRatings", `${POST_ID}_${RATER_UID}_new`),
        makeRatingData({ rating: 5 })
      )
    );
  });

  test("Puan sahibi kendi puanını istemciden güncelleyemez", async () => {
    // ratePost Cloud Function üzerinden çağrılmalı
    const ctx = authenticatedContext(RATER_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "postRatings", RATING_ID), { rating: 2 })
    );
  });

  test("Başka kullanıcı puan belgesi oluşturamaz", async () => {
    const ctx = authenticatedContext(OTHER_UID);
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "postRatings", `${POST_ID}_${OTHER_UID}`),
        makeRatingData({ userId: OTHER_UID })
      )
    );
  });

  test("Admin istemciden puan belgesi oluşturamaz (write: if false)", async () => {
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "postRatings", `${POST_ID}_${ADMIN_UID}`),
        makeRatingData({ userId: ADMIN_UID })
      )
    );
  });

  test("Kimliği doğrulanmamış kullanıcı puan belgesi oluşturamaz", async () => {
    const ctx = unauthenticatedContext();
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "postRatings", `${POST_ID}_anon`),
        makeRatingData()
      )
    );
  });

  test("Kimliği doğrulanmış kullanıcı puan belgesi silemez", async () => {
    const ctx = authenticatedContext(RATER_UID);
    const db = ctx.firestore();
    await assertFails(deleteDoc(doc(db, "postRatings", RATING_ID)));
  });

  test("Admin puan belgesini silemez (write: if false)", async () => {
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertFails(deleteDoc(doc(db, "postRatings", RATING_ID)));
  });
});