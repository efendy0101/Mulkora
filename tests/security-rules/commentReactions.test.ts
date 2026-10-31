/**
 * commentReactions.test.ts — Firestore Güvenlik Kuralları: /commentReactions/{reactionId}
 *
 * docs/15-security-rules-approach.md — commentReactions güvenlik kuralları.
 *
 * KURAL (firestore.rules):
 *   match /commentReactions/{reactionId}
 *     allow read:  if isAuthenticated();
 *     allow write: if false;  // Cloud Function üzerinden
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

const COMMENT_ID = "comment_abc123";
const USER_UID = "user_001";
const OTHER_UID = "user_002";
const MODERATOR_UID = "moderator_001";
const ADMIN_UID = "admin_001";

// reactionId = "{commentId}_{userId}"
const REACTION_ID = `${COMMENT_ID}_${USER_UID}`;

const makeReactionData = (overrides: Record<string, unknown> = {}) => ({
  commentId: COMMENT_ID,
  userId: USER_UID,
  type: "like",
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

  // Cloud Functions Admin SDK simülasyonu ile tepki kaydı oluştur
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, "commentReactions", REACTION_ID), makeReactionData());
  });
});

// ============================================================
// OKUMA TESTLERİ — read: if isAuthenticated()
// ============================================================

describe("commentReactions okuma kuralları", () => {
  test("Kimliği doğrulanmamış kullanıcı yorum tepkisi okuyamaz", async () => {
    const ctx = unauthenticatedContext();
    const db = ctx.firestore();
    await assertFails(getDoc(doc(db, "commentReactions", REACTION_ID)));
  });

  test("Tepki sahibi kendi tepkisini okuyabilir", async () => {
    const ctx = authenticatedContext(USER_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "commentReactions", REACTION_ID)));
  });

  test("Başka kullanıcı tepki belgesini okuyabilir (isAuthenticated)", async () => {
    const ctx = authenticatedContext(OTHER_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "commentReactions", REACTION_ID)));
  });

  test("Moderatör tepki belgesini okuyabilir", async () => {
    const ctx = moderatorContext(MODERATOR_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "commentReactions", REACTION_ID)));
  });

  test("Admin tepki belgesini okuyabilir", async () => {
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "commentReactions", REACTION_ID)));
  });

  test("Engellenen kullanıcı tepki belgesini okuyabilir (isAuthenticated)", async () => {
    const ctx = bannedUserContext(OTHER_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "commentReactions", REACTION_ID)));
  });
});

// ============================================================
// YAZMA TESTLERİ — write: if false
// ============================================================

describe("commentReactions yazma kuralları (write: if false)", () => {
  test("Kimliği doğrulanmış kullanıcı istemciden tepki oluşturamaz", async () => {
    const ctx = authenticatedContext(USER_UID);
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "commentReactions", `${COMMENT_ID}_${USER_UID}_new`),
        makeReactionData()
      )
    );
  });

  test("Kullanıcı kendi tepkisini istemciden güncelleyemez", async () => {
    // toggleCommentLike Cloud Function üzerinden çağrılmalı
    const ctx = authenticatedContext(USER_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "commentReactions", REACTION_ID), {
        type: "dislike",
      })
    );
  });

  test("Başka kullanıcı tepki belgesi oluşturamaz", async () => {
    const ctx = authenticatedContext(OTHER_UID);
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "commentReactions", `${COMMENT_ID}_${OTHER_UID}`),
        makeReactionData({ userId: OTHER_UID })
      )
    );
  });

  test("Moderatör istemciden tepki belgesi oluşturamaz (write: if false)", async () => {
    const ctx = moderatorContext(MODERATOR_UID);
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "commentReactions", `${COMMENT_ID}_${MODERATOR_UID}`),
        makeReactionData({ userId: MODERATOR_UID })
      )
    );
  });

  test("Admin istemciden tepki belgesi oluşturamaz (write: if false)", async () => {
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "commentReactions", `${COMMENT_ID}_${ADMIN_UID}`),
        makeReactionData({ userId: ADMIN_UID })
      )
    );
  });

  test("Kimliği doğrulanmamış kullanıcı tepki belgesi oluşturamaz", async () => {
    const ctx = unauthenticatedContext();
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "commentReactions", "anon_reaction"),
        makeReactionData()
      )
    );
  });

  test("Kullanıcı tepki belgesi silemez (write: if false)", async () => {
    const ctx = authenticatedContext(USER_UID);
    const db = ctx.firestore();
    await assertFails(deleteDoc(doc(db, "commentReactions", REACTION_ID)));
  });

  test("Admin istemciden tepki belgesini silemez (write: if false)", async () => {
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertFails(deleteDoc(doc(db, "commentReactions", REACTION_ID)));
  });
});