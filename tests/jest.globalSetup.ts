/**
 * Jest global setup — Firebase Emulator Suite bağlantısı.
 *
 * Testler çalıştırılmadan önce emulator'ün açık olduğunu doğrular.
 * `firebase emulators:start --only firestore,auth` komutunu çalıştırdıktan sonra bu testleri çalıştırın.
 */
export default async function globalSetup() {
  process.env.FIRESTORE_EMULATOR_HOST = "127.0.0.1:8080";
  process.env.FIREBASE_AUTH_EMULATOR_HOST = "127.0.0.1:9099";
  process.env.GCLOUD_PROJECT = "mulkora-test";
}