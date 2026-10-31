import {
  initializeTestEnvironment,
  RulesTestEnvironment,
  RulesTestContext,
} from "@firebase/rules-unit-testing";
import * as fs from "fs";
import * as path from "path";

const RULES_PATH = path.resolve(__dirname, "../../firestore.rules");
const PROJECT_ID = "mulkora-test";

let testEnv: RulesTestEnvironment;

export async function setupTestEnvironment(): Promise<RulesTestEnvironment> {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync(RULES_PATH, "utf8"),
      host: "127.0.0.1",
      port: 8080,
    },
  });
  return testEnv;
}

export async function teardownTestEnvironment(): Promise<void> {
  await testEnv?.cleanup();
}

export async function clearFirestoreData(): Promise<void> {
  await testEnv?.clearFirestore();
}

/**
 * Kimliği doğrulanmamış bağlam
 */
export function unauthenticatedContext(): RulesTestContext {
  return testEnv.unauthenticatedContext();
}

/**
 * Normal kullanıcı bağlamı
 */
export function authenticatedContext(
  uid: string,
  overrides: Record<string, unknown> = {}
): RulesTestContext {
  return testEnv.authenticatedContext(uid, {
    role: "user",
    banned: false,
    ...overrides,
  });
}

/**
 * Moderatör bağlamı
 */
export function moderatorContext(uid: string): RulesTestContext {
  return testEnv.authenticatedContext(uid, { role: "moderator", banned: false });
}

/**
 * Admin bağlamı
 */
export function adminContext(uid: string): RulesTestContext {
  return testEnv.authenticatedContext(uid, { role: "admin", banned: false });
}

/**
 * Engellenen kullanıcı bağlamı
 */
export function bannedUserContext(uid: string): RulesTestContext {
  return testEnv.authenticatedContext(uid, { role: "user", banned: true });
}

/**
 * Admin SDK (rules'ı atlayan) bağlam — başlangıç verisi yazmak için
 */
