import * as admin from "firebase-admin";
import * as functions from "firebase-functions";
export function publicProfile(uid: string, data: FirebaseFirestore.DocumentData) {
  const fields = ["username", "displayName", "photoUrl", "avatarUrl", "bio", "city", "role", "links", "postCount", "referenceCount", "averageReferenceRating", "ratingCount", "createdAt", "updatedAt"];
  const result: FirebaseFirestore.DocumentData = { uid };
  for (const field of fields) if (data[field] !== undefined) result[field] = data[field];
  return result;
}
export const syncPublicProfile = functions.region("europe-west1").firestore.document("users/{uid}")
  .onWrite(async (change, context) => {
    const ref = admin.firestore().collection("publicProfiles").doc(context.params.uid);
    if (!change.after.exists) { await ref.delete(); return; }
    await ref.set(publicProfile(context.params.uid, change.after.data()!));
  });
