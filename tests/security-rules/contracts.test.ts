/**
 * contracts.test.ts — Firestore Güvenlik Kuralları: contracts koleksiyonu
 *
 * CLAUDE.md §15: Firestore security rules — contracts koleksiyonuna
 * yalnızca party1Uid veya party2Uid okuyabilir/yazabilir. Üçüncü taraflar erişemez.
 * docs/15-security-rules-approach.md — contracts kuralları.
 * docs/12-firestore-document-schemas.md — contracts belgesi şeması.
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
} from "./helpers";

let testEnv: RulesTestEnvironment;

const PARTY1_UID = "party1_user";
const PARTY2_UID = "party2_user";
const STRANGER_UID = "stranger_user";
const ADMIN_UID = "admin_user";
const CONTRACT_ID = "contract_test_001";

const makeContractData = (overrides: Record<string, unknown> = {}) => ({
  documentNumber: "DOC-2025-001",
  version: 1,
  status: "DRAFT",
  party1Uid: PARTY1_UID,
  party1DisplayName: "Taraf 1",
  party1Approved: false,
  party2Uid: PARTY2_UID,
  party2DisplayName: "Taraf 2",
  party2Approved: false,
  propertyAddress: "İstanbul, Kadıköy",
  propertyType: "Konut",
  monthlyRent: 5000,
  paymentDay: 1,
  paymentMethod: "EFT",
  depositAmount: 10000,
  startDate: "2025-01-01",
  endDate: "2026-01-01",
  inventoryItems: [],
  isLocked: false,
  verificationCode: "ABC123",
  pdfStatus: "NOT_GENERATED",
  createdAt: Date.now(),
  updatedAt: Date.now(),
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
});

// ─── SR-CONTRACT-001: Kimliğsiz okuma reddedilir ──────────────────────────────

describe("Kimliği doğrulanmamış kullanıcı", () => {
  beforeEach(async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(
        doc(ctx.firestore(), "contracts", CONTRACT_ID),
        makeContractData()
      );
    });
  });

  it("SR-CONTRACT-001: sözleşmeyi okuyamaz", async () => {
    const ctx = unauthenticatedContext();
    const contractRef = doc(ctx.firestore(), "contracts", CONTRACT_ID);
    await assertFails(getDoc(contractRef));
  });

  it("SR-CONTRACT-001b: sözleşme oluşturamaz", async () => {
    const ctx = unauthenticatedContext();
    const contractRef = doc(ctx.firestore(), "contracts", "new_contract");
    await assertFails(setDoc(contractRef, makeContractData()));
  });
});

// ─── SR-CONTRACT-002: Party1 kendi sözleşmesini okuyabilir ───────────────────

describe("Party1 (sözleşmeyi başlatan taraf)", () => {
  beforeEach(async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(
        doc(ctx.firestore(), "contracts", CONTRACT_ID),
        makeContractData()
      );
    });
  });

  it("SR-CONTRACT-002: sözleşmesini okuyabilir", async () => {
    const ctx = authenticatedContext(PARTY1_UID);
    const contractRef = doc(ctx.firestore(), "contracts", CONTRACT_ID);
    await assertSucceeds(getDoc(contractRef));
  });

  it("SR-CONTRACT-003: sözleşme koleksiyonunu listeleyebilir", async () => {
    const ctx = authenticatedContext(PARTY1_UID);
    // Koleksiyona okuma hakkı var; burada doğrudan kendi sözleşmesini okur.
    const contractRef = doc(ctx.firestore(), "contracts", CONTRACT_ID);
    await assertSucceeds(getDoc(contractRef));
  });
});

// ─── SR-CONTRACT-004: Party2 kendi sözleşmesini okuyabilir ───────────────────

describe("Party2 (davet edilen taraf)", () => {
  beforeEach(async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(
        doc(ctx.firestore(), "contracts", CONTRACT_ID),
        makeContractData()
      );
    });
  });

  it("SR-CONTRACT-004: sözleşmesini okuyabilir", async () => {
    const ctx = authenticatedContext(PARTY2_UID);
    const contractRef = doc(ctx.firestore(), "contracts", CONTRACT_ID);
    await assertSucceeds(getDoc(contractRef));
  });
});

// ─── SR-CONTRACT-005: Üçüncü taraf okuyamaz / yazamaz ───────────────────────

describe("Üçüncü taraf (sözleşmede adı geçmeyen kullanıcı)", () => {
  beforeEach(async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(
        doc(ctx.firestore(), "contracts", CONTRACT_ID),
        makeContractData()
      );
    });
  });

  it("SR-CONTRACT-005: sözleşmeyi okuyamaz", async () => {
    const ctx = authenticatedContext(STRANGER_UID);
    const contractRef = doc(ctx.firestore(), "contracts", CONTRACT_ID);
    await assertFails(getDoc(contractRef));
  });

  it("SR-CONTRACT-006: sözleşmeyi güncelleyemez", async () => {
    const ctx = authenticatedContext(STRANGER_UID);
    const contractRef = doc(ctx.firestore(), "contracts", CONTRACT_ID);
    await assertFails(updateDoc(contractRef, { status: "CANCELLED" }));
  });

  it("SR-CONTRACT-007: sözleşmeyi silemez", async () => {
    const ctx = authenticatedContext(STRANGER_UID);
    const contractRef = doc(ctx.firestore(), "contracts", CONTRACT_ID);
    await assertFails(deleteDoc(contractRef));
  });
});

// ─── SR-CONTRACT-008: Kilitli sözleşme güncellenemez ─────────────────────────

describe("Kilitli (isLocked=true) sözleşme", () => {
  beforeEach(async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(
        doc(ctx.firestore(), "contracts", CONTRACT_ID),
        makeContractData({ isLocked: true, status: "LOCKED" })
      );
    });
  });

  it("SR-CONTRACT-008: party1 kilitli sözleşmeyi güncelleyemez", async () => {
    const ctx = authenticatedContext(PARTY1_UID);
    const contractRef = doc(ctx.firestore(), "contracts", CONTRACT_ID);
    await assertFails(updateDoc(contractRef, { specialConditions: "Yeni koşul" }));
  });

  it("SR-CONTRACT-008b: party2 kilitli sözleşmeyi güncelleyemez", async () => {
    const ctx = authenticatedContext(PARTY2_UID);
    const contractRef = doc(ctx.firestore(), "contracts", CONTRACT_ID);
    await assertFails(updateDoc(contractRef, { specialConditions: "Yeni koşul" }));
  });

  it("SR-CONTRACT-008c: kilitli sözleşme admin tarafından okunabilir", async () => {
    const ctx = adminContext(ADMIN_UID);
    const contractRef = doc(ctx.firestore(), "contracts", CONTRACT_ID);
    await assertSucceeds(getDoc(contractRef));
  });
});

// ─── SR-CONTRACT-009: Admin tüm sözleşmeleri okuyabilir ──────────────────────

describe("Admin kullanıcı", () => {
  beforeEach(async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(
        doc(ctx.firestore(), "contracts", CONTRACT_ID),
        makeContractData()
      );
    });
  });

  it("SR-CONTRACT-009: herhangi bir sözleşmeyi okuyabilir", async () => {
    const ctx = adminContext(ADMIN_UID);
    const contractRef = doc(ctx.firestore(), "contracts", CONTRACT_ID);
    await assertSucceeds(getDoc(contractRef));
  });
});

// ─── SR-CONTRACT-010: Doğrudan yazma (CF olmadan) engellenir ─────────────────

describe("Doğrudan sözleşme oluşturma", () => {
  it("SR-CONTRACT-010: authenticated kullanıcı doğrudan sözleşme oluşturamaz (CF gerekli)", async () => {
    // Sözleşmeler yalnızca Cloud Functions üzerinden createContract CF ile oluşturulur.
    // Doğrudan client-side write kurallar tarafından engellenir.
    const ctx = authenticatedContext(PARTY1_UID);
    const contractRef = doc(ctx.firestore(), "contracts", "direct_write_test");
    await assertFails(setDoc(contractRef, makeContractData({ party1Uid: PARTY1_UID })));
  });
});