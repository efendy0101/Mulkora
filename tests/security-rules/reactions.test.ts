/**
 * reactions.test.ts — Firestore Güvenlik Kuralları: /postReactions/{reactionId}
 *
 * CLAUDE.md §13: Tepkiler — kimliği doğrulanmış kullanıcılar gönderilere tepki
 * verebilir. Yazma işlemleri tamamen Cloud Functions (togglePostReaction / ratePost)
 * üzerinden gerçekleştirilir.
 * docs/15-security-rules-approach.md — postReactions güvenlik kuralları.
 *
 * KURAL (firestore.rules):
 *   match /postReactions/{reactionId}
 *     allow read:  if isAuthenticated();   // giriş yapan herkes okuyabilir
 *     allow write: if false;               // yalnızca Cloud Functions
 *
 * NOT: Kural isAuthenticated() kullanır — banned kontrolü yoktur.
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

const REACTOR_UID = "reactor_user_001";
const OTHER_UID = "other_user_002";
const MODERATOR_UID = "moderator_001";
const ADMIN_UID = "admin_001";
const POST_ID = "post_test_001";
// postReactions doküman ID formatı: "{postId}_{uid}"
const REACTION_ID = `${POST_ID}_${REACTOR_UID}`;

const makeReactionData = (overrides: Record<string, unknown> = {}) => ({
  postId: POST_ID,
  uid: REACTOR_UID,
  type: "upvote",
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

  // Cloud Functions Admin SDK simülasyonu ile test verisi oluştur
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();

    // Onaylanmış gönderi
    await setDoc(doc(db, "posts", POST_ID), {
      status: "approved",
      authorUid: "post_author",
      title: "Test gönderisi",
      upvoteCount: 1,
      downvoteCount: 0,
      createdAt: new Date(),
    });

    // Mevcut tepki belgesi (CF tarafından oluşturulmuş gibi)
    await setDoc(doc(db, "postReactions", REACTION_ID), makeReactionData());
  });
});

// ============================================================
// OKUMA TESTLERİ
// ============================================================

describe("Tepki okuma kuralları", () => {
  test("Kimliği doğrulanmamış kullanıcı tepkileri okuyamaz", async () => {
    // Kural: allow read: if isAuthenticated() → anon reddedilir
    const ctx = unauthenticatedContext();
    const db = ctx.firestore();
    await assertFails(getDoc(doc(db, "postReactions", REACTION_ID)));
  });

  test("Kimliği doğrulanmış kullanıcı tepkileri okuyabilir", async () => {
    const ctx = authenticatedContext(OTHER_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "postReactions", REACTION_ID)));
  });

  test("Tepki sahibi kendi tepkisini okuyabilir", async () => {
    const ctx = authenticatedContext(REACTOR_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "postReactions", REACTION_ID)));
  });

  test("Moderatör tepkileri okuyabilir", async () => {
    const ctx = moderatorContext(MODERATOR_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "postReactions", REACTION_ID)));
  });

  test("Admin tepkileri okuyabilir", async () => {
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "postReactions", REACTION_ID)));
  });

  test("Engellenmiş kullanıcı tepkileri okuyabilir", async () => {
    // Kural: allow read: if isAuthenticated() — ban kontrolü yoktur.
    // bannedUserContext isAuthenticated() koşulunu karşılar.
    const ctx = bannedUserContext(OTHER_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "postReactions", REACTION_ID)));
  });
});

// ============================================================
// YAZMA TESTLERİ — write: if false (tüm istemci yazması engelli)
// ============================================================

describe("Tepki oluşturma kuralları (write: if false)", () => {
  test("Kimliği doğrulanmış kullanıcı doğrudan tepki oluşturamaz", async () => {
    // CLAUDE.md §25: togglePostReaction Cloud Function üzerinden çağrılmalı.
    const ctx = authenticatedContext(OTHER_UID);
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "postReactions", `${POST_ID}_${OTHER_UID}`),
        makeReactionData({ uid: OTHER_UID })
      )
    );
  });

  test("Kimliği doğrulanmamış kullanıcı tepki oluşturamaz", async () => {
    const ctx = unauthenticatedContext();
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "postReactions", `${POST_ID}_anon`),
        makeReactionData({ uid: "anon" })
      )
    );
  });

  test("Moderatör istemciden tepki oluşturamaz", async () => {
    const ctx = moderatorContext(MODERATOR_UID);
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "postReactions", `${POST_ID}_${MODERATOR_UID}`),
        makeReactionData({ uid: MODERATOR_UID })
      )
    );
  });

  test("Admin istemciden tepki oluşturamaz (write: if false)", async () => {
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "postReactions", `${POST_ID}_${ADMIN_UID}`),
        makeReactionData({ uid: ADMIN_UID })
      )
    );
  });
});

describe("Tepki güncelleme kuralları (write: if false)", () => {
  test("Tepki sahibi tepkisini istemciden güncelleyemez", async () => {
    const ctx = authenticatedContext(REACTOR_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "postReactions", REACTION_ID), {
        type: "downvote",
      })
    );
  });

  test("Başka kullanıcı tepkiyi güncelleyemez", async () => {
    const ctx = authenticatedContext(OTHER_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "postReactions", REACTION_ID), {
        type: "downvote",
      })
    );
  });

  test("Moderatör tepkiyi istemciden güncelleyemez", async () => {
    const ctx = moderatorContext(MODERATOR_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "postReactions", REACTION_ID), {
        type: "downvote",
      })
    );
  });
});

describe("Tepki silme kuralları (write: if false)", () => {
  test("Tepki sahibi tepkisini istemciden silemez", async () => {
    const ctx = authenticatedContext(REACTOR_UID);
    const db = ctx.firestore();
    await assertFails(deleteDoc(doc(db, "postReactions", REACTION_ID)));
  });

  test("Başka kullanıcı tepkiyi silemez", async () => {
    const ctx = authenticatedContext(OTHER_UID);
    const db = ctx.firestore();
    await assertFails(deleteDoc(doc(db, "postReactions", REACTION_ID)));
  });

  test("Kimliği doğrulanmamış kullanıcı tepkiyi silemez", async () => {
    const ctx = unauthenticatedContext();
    const db = ctx.firestore();
    await assertFails(deleteDoc(doc(db, "postReactions", REACTION_ID)));
  });

  test("Admin tepkiyi istemciden silemez (write: if false)", async () => {
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertFails(deleteDoc(doc(db, "postReactions", REACTION_ID)));
  });
});