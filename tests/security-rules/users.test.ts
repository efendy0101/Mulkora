/**
 * users.test.ts — Firestore Güvenlik Kuralları: /users/{userId} ve /usernames/{username}
 *
 * CLAUDE.md §26: Firestore ve Storage Güvenliği
 * docs/15-security-rules-approach.md — users ve usernames koleksiyon kuralları
 *
 * KURALAR (firestore.rules):
 *   /users/{userId}
 *     allow read:  if isAuthenticated();
 *     allow create: if false;  // Cloud Function: createUserProfile (CF-AUTH-001)
 *     allow update: if isOwner(userId) && !request.resource.data.keys().hasAny(["role","adsRemoved","isBanned","postCount","referenceScore"]);
 *     allow delete: if false;  // Cloud Function: deleteAccount (CF-AUTH-003)
 *
 *   /usernames/{username}
 *     allow read:  if isAuthenticated();
 *     allow write: if false;  // Cloud Function: checkUsername (CF-AUTH-002)
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

const USER_A_UID = "user_a_001";
const USER_B_UID = "user_b_002";
const MODERATOR_UID = "moderator_001";
const ADMIN_UID = "admin_001";

const makeUserData = (uid: string, overrides: Record<string, unknown> = {}) => ({
  uid,
  username: `user_${uid.replace(/[^a-z0-9]/gi, "")}`,
  displayName: "Test Kullanıcısı",
  email: `${uid}@test.com`,
  role: "tenant",
  city: "İstanbul",
  bio: "",
  avatarUrl: null,
  isBanned: false,
  adsRemoved: false,
  postCount: 0,
  referenceScore: 0,
  fcmTokens: [],
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

  // Cloud Functions Admin SDK simülasyonu ile kullanıcı profilleri oluştur
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, "users", USER_A_UID), makeUserData(USER_A_UID));
    await setDoc(doc(db, "users", USER_B_UID), makeUserData(USER_B_UID));
    await setDoc(doc(db, "usernames", `user_${USER_A_UID.replace(/[^a-z0-9]/gi, "")}`), {
      uid: USER_A_UID,
      createdAt: new Date(),
    });
  });
});

// ============================================================
// /users/{userId} OKUMA TESTLERİ
// ============================================================

describe("users/{userId} okuma kuralları", () => {
  test("Kimliği doğrulanmamış kullanıcı profil okuyamaz", async () => {
    const ctx = unauthenticatedContext();
    const db = ctx.firestore();
    await assertFails(getDoc(doc(db, "users", USER_A_UID)));
  });

  test("Kimliği doğrulanmış kullanıcı kendi profilini okuyabilir", async () => {
    const ctx = authenticatedContext(USER_A_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "users", USER_A_UID)));
  });

  test("Kimliği doğrulanmış kullanıcı başka özel profili okuyamaz", async () => {
    const ctx = authenticatedContext(USER_A_UID);
    const db = ctx.firestore();
    await assertFails(getDoc(doc(db, "users", USER_B_UID)));
  });

  test("Moderatör kullanıcı profilini okuyabilir", async () => {
    const ctx = moderatorContext(MODERATOR_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "users", USER_A_UID)));
  });

  test("Admin kullanıcı profilini okuyabilir", async () => {
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "users", USER_A_UID)));
  });

  test("Engellenen kullanıcı başkasının özel profilini okuyamaz", async () => {
    const ctx = bannedUserContext(USER_B_UID);
    const db = ctx.firestore();
    await assertFails(getDoc(doc(db, "users", USER_A_UID)));
  });
});

// ============================================================
// /users/{userId} YAZMA TESTLERİ
// ============================================================

describe("users/{userId} oluşturma kuralları (create: if false)", () => {
  test("Kimliği doğrulanmış kullanıcı istemciden profil oluşturamaz", async () => {
    const ctx = authenticatedContext(USER_A_UID);
    const db = ctx.firestore();
    await assertFails(
      setDoc(doc(db, "users", "new_user_xxx"), makeUserData("new_user_xxx"))
    );
  });

  test("Admin istemciden profil oluşturamaz (create: if false)", async () => {
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertFails(
      setDoc(doc(db, "users", "admin_created_user"), makeUserData("admin_created_user"))
    );
  });

  test("Kimliği doğrulanmamış kullanıcı profil oluşturamaz", async () => {
    const ctx = unauthenticatedContext();
    const db = ctx.firestore();
    await assertFails(
      setDoc(doc(db, "users", USER_A_UID), makeUserData(USER_A_UID))
    );
  });
});

test("Kullanıcı kendisi için admin profili oluşturamaz", async () => {
  const db = authenticatedContext("new_user").firestore();
  await assertFails(setDoc(doc(db, "users", "new_user"), makeUserData("new_user", { role: "admin" })));
});

test("Özel alanlar olmadan genel profil okunabilir, istemci yazamaz", async () => {
  await testEnv.withSecurityRulesDisabled(async ctx => setDoc(doc(ctx.firestore(), "publicProfiles", USER_B_UID), {uid: USER_B_UID, displayName: "Demo"}));
  const db = authenticatedContext(USER_A_UID).firestore();
  await assertSucceeds(getDoc(doc(db, "publicProfiles", USER_B_UID)));
  await assertFails(setDoc(doc(db, "publicProfiles", USER_A_UID), {role: "admin"}));
});

describe("users/{userId} güncelleme kuralları", () => {
  test("Kullanıcı kendi izin verilen profilini güncelleyebilir", async () => {
    const ctx = authenticatedContext(USER_A_UID);
    const db = ctx.firestore();
    await assertSucceeds(
      updateDoc(doc(db, "users", USER_A_UID), {
        displayName: "Yeni Ad",
        bio: "Yeni biyografi",
        city: "Ankara",
      })
    );
  });

  test("Kullanıcı başka kullanıcının profilini güncelleyemez", async () => {
    const ctx = authenticatedContext(USER_A_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "users", USER_B_UID), {
        displayName: "Saldırgan Ad",
      })
    );
  });

  test("Kullanıcı kendi rolünü değiştiremez (role alanı korumalı)", async () => {
    const ctx = authenticatedContext(USER_A_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "users", USER_A_UID), {
        role: "admin",
      })
    );
  });

  test("Kullanıcı moderatör rolü veremez kendine", async () => {
    const ctx = authenticatedContext(USER_A_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "users", USER_A_UID), {
        role: "moderator",
      })
    );
  });

  test("Kullanıcı kendi adsRemoved alanını değiştiremez (korumalı)", async () => {
    const ctx = authenticatedContext(USER_A_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "users", USER_A_UID), {
        adsRemoved: true,
      })
    );
  });

  test("Kullanıcı isBanned alanını değiştiremez (korumalı)", async () => {
    const ctx = authenticatedContext(USER_A_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "users", USER_A_UID), {
        isBanned: true,
      })
    );
  });

  test("Kullanıcı postCount sayacını doğrudan değiştiremez (korumalı)", async () => {
    const ctx = authenticatedContext(USER_A_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "users", USER_A_UID), {
        postCount: 999,
      })
    );
  });

  test("Kullanıcı referenceScore alanını değiştiremez (korumalı)", async () => {
    const ctx = authenticatedContext(USER_A_UID);
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "users", USER_A_UID), {
        referenceScore: 100,
      })
    );
  });

  test("Kimliği doğrulanmamış kullanıcı profil güncelleyemez", async () => {
    const ctx = unauthenticatedContext();
    const db = ctx.firestore();
    await assertFails(
      updateDoc(doc(db, "users", USER_A_UID), {
        displayName: "Anonim Saldırı",
      })
    );
  });
});

describe("users/{userId} silme kuralları (delete: if false)", () => {
  test("Kimliği doğrulanmış kullanıcı kendi profilini silemez", async () => {
    const ctx = authenticatedContext(USER_A_UID);
    const db = ctx.firestore();
    await assertFails(deleteDoc(doc(db, "users", USER_A_UID)));
  });

  test("Admin kullanıcı profili silebilir", async () => {
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertSucceeds(deleteDoc(doc(db, "users", USER_A_UID)));
  });
});

// ============================================================
// /usernames/{username} TESTLERİ
// ============================================================

describe("usernames/{username} okuma kuralları", () => {
  const USERNAME = `user_${USER_A_UID.replace(/[^a-z0-9]/gi, "")}`;

  test("Kimliği doğrulanmamış kullanıcı username belgesi okuyamaz", async () => {
    const ctx = unauthenticatedContext();
    const db = ctx.firestore();
    await assertFails(getDoc(doc(db, "usernames", USERNAME)));
  });

  test("Kimliği doğrulanmış kullanıcı username belgesini okuyabilir", async () => {
    const ctx = authenticatedContext(USER_A_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "usernames", USERNAME)));
  });

  test("Admin username belgesini okuyabilir", async () => {
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertSucceeds(getDoc(doc(db, "usernames", USERNAME)));
  });
});

describe("usernames/{username} yazma kuralları (write: if false)", () => {
  test("Kimliği doğrulanmış kullanıcı username belgesi oluşturamaz", async () => {
    const ctx = authenticatedContext(USER_A_UID);
    const db = ctx.firestore();
    await assertFails(
      setDoc(doc(db, "usernames", "yeni_kullanici_adi"), {
        uid: USER_A_UID,
        createdAt: new Date(),
      })
    );
  });

  test("Admin istemciden username belgesi oluşturamaz (write: if false)", async () => {
    const ctx = adminContext(ADMIN_UID);
    const db = ctx.firestore();
    await assertFails(
      setDoc(doc(db, "usernames", "admin_username"), {
        uid: ADMIN_UID,
        createdAt: new Date(),
      })
    );
  });

  test("Kimliği doğrulanmamış kullanıcı username belgesi oluşturamaz", async () => {
    const ctx = unauthenticatedContext();
    const db = ctx.firestore();
    await assertFails(
      setDoc(doc(db, "usernames", "anon_username"), {
        uid: "anon",
        createdAt: new Date(),
      })
    );
  });

  test("Kimliği doğrulanmış kullanıcı username belgesi silemez", async () => {
    const USERNAME = `user_${USER_A_UID.replace(/[^a-z0-9]/gi, "")}`;
    const ctx = authenticatedContext(USER_A_UID);
    const db = ctx.firestore();
    await assertFails(deleteDoc(doc(db, "usernames", USERNAME)));
  });
});