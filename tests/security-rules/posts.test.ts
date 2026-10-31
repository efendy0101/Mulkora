/**
 * posts.test.ts — Firestore Güvenlik Kuralları: /posts/{postId}
 *
 * CLAUDE.md §8: Gönderi Onay Sistemi
 * CLAUDE.md §26: Firestore ve Storage Güvenliği
 * docs/17-post-moderation-state-machine.md
 *
 * KURALAR (firestore.rules):
 *   /posts/{postId}
 *     allow read:    if isApproved(resource) || isOwner(resource.authorUid) || isModerator() || isAdmin();
 *     allow create:  if false;   // submitPost Cloud Function (CF-POST-001)
 *     allow update:  if false;   // approvePost / rejectPost / requestEdit Cloud Functions
 *     allow delete:  if isAdmin();
 *
 *   Sayaç alanları (likeCount, dislikeCount, commentCount, ratingAvg) hiçbir zaman
 *   istemci tarafından değiştirilemez.
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

const AUTHOR_UID = "author_001";
const READER_UID = "reader_002";
const MODERATOR_UID = "moderator_001";
const ADMIN_UID = "admin_001";

// --- Veri Fabrikaları ---

const makePostData = (
  status: string,
  authorUid: string,
  overrides: Record<string, unknown> = {}
) => ({
  authorUid,
  authorUsername: "test_user",
  title: "Test Gönderisi",
  content: "Test içeriği.",
  postType: "experience",
  category: "deposit",
  city: "İstanbul",
  status,
  isAnonymous: false,
  commentsEnabled: true,
  likeCount: 0,
  dislikeCount: 0,
  commentCount: 0,
  ratingAvg: 0,
  ratingCount: 0,
  isFlagged: false,
  imageUrl: null,
  createdAt: new Date(),
  updatedAt: new Date(),
  ...overrides,
});

const POST_APPROVED = "post_approved_001";
const POST_PENDING = "post_pending_002";
const POST_REJECTED = "post_rejected_003";
const POST_DRAFT = "post_draft_004";
const POST_OTHER_USER = "post_other_user_005";

beforeAll(async () => {
  testEnv = await setupTestEnvironment();
});

afterAll(async () => {
  await teardownTestEnvironment();
});

beforeEach(async () => {
  await clearFirestoreData();

  // Cloud Functions Admin SDK simülasyonu ile çeşitli durumlarda gönderiler oluştur
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(
      doc(db, "posts", POST_APPROVED),
      makePostData("approved", AUTHOR_UID)
    );
    await setDoc(
      doc(db, "posts", POST_PENDING),
      makePostData("pending", AUTHOR_UID)
    );
    await setDoc(
      doc(db, "posts", POST_REJECTED),
      makePostData("rejected", AUTHOR_UID)
    );
    await setDoc(
      doc(db, "posts", POST_DRAFT),
      makePostData("draft", AUTHOR_UID)
    );
    await setDoc(
      doc(db, "posts", POST_OTHER_USER),
      makePostData("pending", READER_UID)
    );
  });
});

// ============================================================
// ONAYLANMIŞ GÖNDERİ — Herkes okuyabilir (isAuthenticated veya public)
// ============================================================

describe("posts: onaylanmış gönderi okuma", () => {
  test("Kimliği doğrulanmamış kullanıcı onaylanmış gönderiyi okuyabilir", async () => {
    const ctx = unauthenticatedContext();
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "posts", POST_APPROVED)));
  });

  test("Kimliği doğrulanmış okuyucu onaylanmış gönderiyi okuyabilir", async () => {
    const ctx = authenticatedContext(READER_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "posts", POST_APPROVED)));
  });

  test("Yazar kendi onaylanmış gönderisini okuyabilir", async () => {
    const ctx = authenticatedContext(AUTHOR_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "posts", POST_APPROVED)));
  });

  test("Moderatör onaylanmış gönderiyi okuyabilir", async () => {
    const ctx = moderatorContext(MODERATOR_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "posts", POST_APPROVED)));
  });

  test("Admin onaylanmış gönderiyi okuyabilir", async () => {
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "posts", POST_APPROVED)));
  });
});

// ============================================================
// BEKLEYEN / TASLAK / REDDEDİLEN GÖNDERİ — Kısıtlı erişim
// ============================================================

describe("posts: bekleyen/taslak/reddedilen gönderi okuma", () => {
  test("Kimliği doğrulanmamış kullanıcı bekleyen gönderiyi okuyamaz", async () => {
    const ctx = unauthenticatedContext();
    const db = ctx.firestore();
    await assertFails(getDoc(doc(db, "posts", POST_PENDING)));
  });

  test("Başka kullanıcı bekleyen gönderiyi okuyamaz", async () => {
    const ctx = authenticatedContext(READER_UID);
    const db = ctx.firestore();
    await assertFails(getDoc(doc(db, "posts", POST_PENDING)));
  });

  test("Yazar kendi bekleyen gönderisini okuyabilir", async () => {
    const ctx = authenticatedContext(AUTHOR_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "posts", POST_PENDING)));
  });

  test("Moderatör bekleyen gönderiyi okuyabilir", async () => {
    const ctx = moderatorContext(MODERATOR_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "posts", POST_PENDING)));
  });

  test("Admin bekleyen gönderiyi okuyabilir", async () => {
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "posts", POST_PENDING)));
  });

  test("Başka kullanıcı taslak gönderiyi okuyamaz", async () => {
    const ctx = authenticatedContext(READER_UID);
    const db = ctx.firestore();
    await assertFails(getDoc(doc(db, "posts", POST_DRAFT)));
  });

  test("Yazar kendi taslak gönderisini okuyabilir", async () => {
    const ctx = authenticatedContext(AUTHOR_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "posts", POST_DRAFT)));
  });

  test("Başka kullanıcı reddedilen gönderiyi okuyamaz", async () => {
    const ctx = authenticatedContext(READER_UID);
    const db = ctx.firestore();
    await assertFails(getDoc(doc(db, "posts", POST_REJECTED)));
  });

  test("Yazar kendi reddedilen gönderisini okuyabilir", async () => {
    const ctx = authenticatedContext(AUTHOR_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "posts", POST_REJECTED)));
  });

  test("Moderatör başka kullanıcının bekleyen gönderisini okuyabilir", async () => {
    const ctx = moderatorContext(MODERATOR_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "posts", POST_OTHER_USER)));
  });
});

// ============================================================
// GÖNDERİ OLUŞTURMA — create: if false
// ============================================================

describe("posts: oluşturma kuralları (create: if false)", () => {
  test("Kimliği doğrulanmış kullanıcı istemciden gönderi oluşturamaz", async () => {
    const ctx = authenticatedContext(AUTHOR_UID);
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "posts", "new_post_xxx"),
        makePostData("draft", AUTHOR_UID)
      )
    );
  });

  test("Admin istemciden gönderi oluşturamaz (create: if false)", async () => {
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "posts", "admin_post_xxx"),
        makePostData("approved", ADMIN_UID)
      )
    );
  });

  test("Kimliği doğrulanmamış kullanıcı gönderi oluşturamaz", async () => {
    const ctx = unauthenticatedContext();
    const db = ctx.firestore();
    await assertFails(
      setDoc(
        doc(db, "posts", "anon_post_xxx"),
        makePostData("draft", "anon_uid")
      )
    );
  });
});

// ============================================================
// GÖNDERİ GÜNCELLEME — update: if false
// ============================================================

describe("posts: güncelleme kuralları (update: if false)", () => {
  test("Yazar kendi gönderisini istemciden güncelleyemez", async () => {
    const ctx = authenticatedContext(AUTHOR_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "posts", POST_DRAFT), {
        title: "Güncellenmiş başlık",
      })
    );
  });

  test("Moderatör gönderi durumunu istemciden değiştiremez", async () => {
    const ctx = moderatorContext(MODERATOR_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "posts", POST_PENDING), {
        status: "approved",
      })
    );
  });

  test("Başka kullanıcı gönderi güncelleyemez", async () => {
    const ctx = authenticatedContext(READER_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "posts", POST_APPROVED), {
        title: "Saldırgan Başlık",
      })
    );
  });

  test("Admin istemciden güncelleme yapamaz (update: if false)", async () => {
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "posts", POST_APPROVED), {
        status: "removed",
      })
    );
  });

  test("Kullanıcı likeCount sayacını istemciden değiştiremez", async () => {
    const ctx = authenticatedContext(READER_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "posts", POST_APPROVED), {
        likeCount: 999,
      })
    );
  });

  test("Kullanıcı dislikeCount sayacını istemciden değiştiremez", async () => {
    const ctx = authenticatedContext(READER_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "posts", POST_APPROVED), {
        dislikeCount: 999,
      })
    );
  });

  test("Kullanıcı commentCount sayacını istemciden değiştiremez", async () => {
    const ctx = authenticatedContext(READER_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "posts", POST_APPROVED), {
        commentCount: 999,
      })
    );
  });

  test("Kullanıcı kendi gönderisinin ratingAvg değerini istemciden değiştiremez", async () => {
    const ctx = authenticatedContext(AUTHOR_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "posts", POST_APPROVED), {
        ratingAvg: 5.0,
      })
    );
  });
});

// ============================================================
// GÖNDERİ SİLME — delete: if isAdmin()
// ============================================================

describe("posts: silme kuralları (delete: if isAdmin())", () => {
  test("Yazar kendi gönderisini istemciden silemez", async () => {
    const ctx = authenticatedContext(AUTHOR_UID);
    const db = ctx.firestore();
    await assertFails(deleteDoc(doc(db, "posts", POST_APPROVED)));
  });

  test("Moderatör gönderiyi istemciden silemez", async () => {
    const ctx = moderatorContext(MODERATOR_UID);
    const db = ctx.firestore();
    await assertFails(deleteDoc(doc(db, "posts", POST_APPROVED)));
  });

  test("Admin gönderiyi silebilir", async () => {
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertSucceeds(deleteDoc(doc(db, "posts", POST_APPROVED)));
  });

  test("Kimliği doğrulanmamış kullanıcı gönderiyi silemez", async () => {
    const ctx = unauthenticatedContext();
    const db = ctx.firestore();
    await assertFails(deleteDoc(doc(db, "posts", POST_APPROVED)));
  });

  test("Engellenen kullanıcı gönderiyi silemez", async () => {
    const ctx = bannedUserContext(READER_UID);
    const db = ctx.firestore();
    await assertFails(deleteDoc(doc(db, "posts", POST_APPROVED)));
  });
});