import { initializeApp as initApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getFirestore, FieldValue as FirestoreFieldValue, Timestamp as FirestoreTimestamp, FieldPath as FirestoreFieldPath } from "firebase-admin/firestore";
import type { Firestore as FirestoreType, DocumentReference as DocumentReferenceType } from "firebase-admin/firestore";
import { getStorage } from "firebase-admin/storage";
import { getMessaging } from "firebase-admin/messaging";
import type { AndroidConfig as AndroidConfigType, ApnsConfig as ApnsConfigType, MulticastMessage as MulticastMessageType, Notification as NotificationType } from "firebase-admin/messaging";
export const initializeApp = initApp;
export const auth = getAuth;
export const storage = getStorage;
export function firestore() { return getFirestore(); }
export namespace firestore {
  export const FieldValue = FirestoreFieldValue;
  export const Timestamp = FirestoreTimestamp;
  export type Timestamp = FirestoreTimestamp;
  export const FieldPath = FirestoreFieldPath;
  export type Firestore = FirestoreType;
  export type DocumentReference = DocumentReferenceType;
}
export function messaging() { return getMessaging(); }
export namespace messaging {
  export type AndroidConfig = AndroidConfigType;
  export type ApnsConfig = ApnsConfigType;
  export type MulticastMessage = MulticastMessageType;
  export type Notification = NotificationType;
}
