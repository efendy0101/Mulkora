/**
 * comments.test.ts — Firestore Güvenlik Kuralları: /posts/{postId}/comments/{commentId}
 *
 * CLAUDE.md §10: Yorumlar — posts alt koleksiyonu olarak saklanır.
 * Tüm yazma işlemleri addComment/deleteComment CF üzerinden yapılır.
 * İstemci tarafı yazma tamamen yasaktır (write: if false).
 * docs/15-security-rules-approach.md — comments koleksiyonu güvenlik kuralları.
 *
 * KURAL (firestore.rules):
 *   match /posts/{postId}/comments/{commentId}
 *     allow read:  if isAuthenticated();   // giriş yapmış herkes okuyabilir
 *     allow write: if false;               // yalnızca Cloud Functions
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

const AUTHOR_UID = "comment_author_001";
const OTHER_UID = "other_user_002";
const MODERATOR_UID = "moderator_001";
const ADMIN_UID = "admin_001";
const POST_ID = "post_test_001";
const COMMENT_ID = "comment_test_001";

const makeCommentData = (overrides: Record<string, unknown> = {}) => ({
  postId: POST_ID,
  authorUid: AUTHOR_UID,
  content: "Bu bir test yorumudur.",
  likeCount: 0,
  isDeleted: false,
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

  // Cloud Functions Admin SDK simülasyonu ile test verisi oluştur
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();

    // Onaylanmış gönderi oluştur
    await setDoc(doc(db, "posts", POST_ID), {
      status: "approved",
      authorUid: "post_author",
      title: "Test gönderisi",
      createdAt: new Date(),
    });

    // Test yorumunu oluştur (CF yaptığı gibi admin SDK ile)
    await setDoc(
      doc(db, "posts", POST_ID, "comments", COMMENT_ID),
      makeCommentData()
    );
  });
});

// ============================================================
// OKUMA TESTLERİ
// ============================================================

describe("Yorum okuma kuralları", () => {
  test("Kimliği doğrulanmamış kullanıcı yorum okuyamaz", async () => {
    // Kural: allow read: if isAuthenticated() → anon erişim reddedilir
    const ctx = unauthenticatedContext();
    const db = ctx.firestore();
    await assertFails(
      getDoc(doc(db, "posts", POST_ID, "comments", COMMENT_ID))
    );
  });

  test("Kimliği doğrulanmış kullanıcı yorumu okuyabilir", async () => {
    const ctx = authenticatedContext(OTHER_UID);
    const db = ctx.firestore();
    await assertSucceeds(
      getDoc(doc(db, "posts", POST_ID, "comments", COMMENT_ID))
    );
  });

  test("Yorum sahibi kendi yorumunu okuyabilir", async () => {
    const ctx = authenticatedContext(AUTHOR_UID);
    const db = ctx.firestore();
    await assertSucceeds(
      getDoc(doc(db, "posts", POST_ID, "comments", COMMENT_ID))
    );
  });

  test("Moderatör yorumları okuyabilir", async () => {
    const ctx = moderatorContext(MODERATOR_UID);
    const db = ctx.firestore();
    await assertSucceeds(
      getDoc(doc(db, "posts", POST_ID, "comments", COMMENT_ID))
    );
  });

  test("Admin yorumları okuyabilir", async () => {
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertSucceeds(
      getDoc(doc(db, "posts", POST_ID, "comments", COMMENT_ID))
    );
  });

  test("Engellenmiş kullanıcı yorumları okuyabilir", async () => {
    // Kural: allow read: if isAuthenticated() — ban kontrolü yoktur.
    // bannedUserContext isAuthenticated() koşulunu karşılar.
    const ctx = bannedUserContext(OTHER_UID);
    const db = ctx.firestore();
    await assertSucceeds(
      getDoc(doc(db, "posts", POST_ID, "comments", COMMENT_ID))
    );
  });
});

// ============================================================
// YAZMA TESTLERİ — write: if false
// ============================================================

describe("Yorum oluşturma kuralları (write: if false)", () => {
  test("Kimliği doğrulanmış kullanıcı istemciden yorum oluşturamaz", async () => {
    // CLAUDE.md §25: addComment Cloud Function üzerinden çağrılmalı.
    const ctx = authenticatedContext(OTHER_UID);
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "posts", POST_ID, "comments", "new_comment_999"),
        makeCommentData({ authorUid: OTHER_UID })
      )
    );
  });

  test("Kimliği doğrulanmamış kullanıcı yorum oluşturamaz", async () => {
    const ctx = unauthenticatedContext();
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "posts", POST_ID, "comments", "new_comment_888"),
        makeCommentData({ authorUid: "anon" })
      )
    );
  });

  test("Moderatör istemciden yorum oluşturamaz", async () => {
    const ctx = moderatorContext(MODERATOR_UID);
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "posts", POST_ID, "comments", "new_comment_777"),
        makeCommentData({ authorUid: MODERATOR_UID })
      )
    );
  });

  test("Admin istemciden yorum oluşturamaz (write: if false)", async () => {
    // Yorum oluşturma dahil tüm yazma işlemleri CF Admin SDK üzerinden yapılır.
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "posts", POST_ID, "comments", "new_comment_666"),
        makeCommentData({ authorUid: ADMIN_UID })
      )
    );
  });
});

describe("Yorum güncelleme kuralları (write: if false)", () => {
  test("Yorum sahibi kendi yorumunu güncelleyemez", async () => {
    const ctx = authenticatedContext(AUTHOR_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "posts", POST_ID, "comments", COMMENT_ID), {
        content: "Değiştirilmiş içerik",
      })
    );
  });

  test("Başka kullanıcı yorumu güncelleyemez", async () => {
    const ctx = authenticatedContext(OTHER_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "posts", POST_ID, "comments", COMMENT_ID), {
        content: "Kötü niyetli güncelleme",
      })
    );
  });

  test("Moderatör yorumu istemciden güncelleyemez", async () => {
    const ctx = moderatorContext(MODERATOR_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "posts", POST_ID, "comments", COMMENT_ID), {
        isDeleted: true,
      })
    );
  });
});

describe("Yorum silme kuralları (write: if false)", () => {
  test("Yorum sahibi kendi yorumunu istemciden silemez", async () => {
    const ctx = authenticatedContext(AUTHOR_UID);
    const db = ctx.firestore();
    await assertFails(
      deleteDoc(doc(db, "posts", POST_ID, "comments", COMMENT_ID))
    );
  });

  test("Başka kullanıcı yorumu silemez", async () => {
    const ctx = authenticatedContext(OTHER_UID);
    const db = ctx.firestore();
    await assertFails(
      deleteDoc(doc(db, "posts", POST_ID, "comments", COMMENT_ID))
    );
  });

  test("Moderatör yorumu istemciden silemez", async () => {
    const ctx = moderatorContext(MODERATOR_UID);
    const db = ctx.firestore();
    await assertFails(
      deleteDoc(doc(db, "posts", POST_ID, "comments", COMMENT_ID))
    );
  });

  test("Admin yorumu istemciden silemez (write: if false)", async () => {
    // Admin SDK kuralları atladığı için bu test istemci erişimini test eder.
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertFails(
      deleteDoc(doc(db, "posts", POST_ID, "comments", COMMENT_ID))
    );
  });
});